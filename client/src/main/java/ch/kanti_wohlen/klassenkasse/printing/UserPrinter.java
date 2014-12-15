package ch.kanti_wohlen.klassenkasse.printing;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.User.NamingMode;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketPrintingInformation;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.PaymentComparator;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class UserPrinter extends PrintBase implements Printable {

	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag("de-ch"));
	private static final int FOOTER_OFFSET = 20;

	private final StudentClass studentClass;
	private final User user;
	private final List<Payment> sortedPayments;
	private final boolean onePage;
	private final JLabel footerLabel;
	private final MonetaryValue maxLenValue;

	// TODO: Improve this argument spam without adding unnecessary network traffic
	@NonNullByDefault
	public UserPrinter(Host host, User user, StudentClass studentClass, PacketPrintingInformation printingInformation,
			Map<String, String> userVariables, boolean onePage) {
		super(printingInformation.getHeaderImage());

		this.studentClass = studentClass;
		this.user = user;
		this.onePage = onePage;

		// Replace default variables
		String footer = printingInformation.getFooterNote().replace("[firstname]", user.getFirstName())
				.replace("[lastname]", user.getLastName())
				.replace("[username]", user.getUsername())
				.replace("[balance]", user.getBalance().getAmountString(true));

		// Replace user variables
		for (Entry<String, String> entry : userVariables.entrySet()) {
			String value = entry.getValue();
			if (value == null || value.isEmpty()) continue;

			String var = "{" + entry.getKey() + "}";
			footer = footer.replace(var, value);
		}

		// Remove the rest of the undefined variables
		footer = footer.replaceAll("\\{.+\\}", "");

		footerLabel = new JLabel(footer);
		//footerLabel.setFont(DEFAULT_FONT);
		footerLabel.setSize(footerLabel.getPreferredSize());

		Collection<Payment> payments = host.getPaymentsByUser(user.getLocalId()).values();
		sortedPayments = new ArrayList<>(payments);
		Collections.sort(sortedPayments, new PaymentComparator());

		MonetaryValue min = MonetaryValue.ZERO;
		for (Payment payment : payments) {
			MonetaryValue negative = payment.getValue();
			if (negative.signum() == 1) negative = negative.negate();

			if (negative.compareTo(min) == -1) {
				min = negative;
			}
		}
		maxLenValue = min;
	}

	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		Graphics2D g = (Graphics2D) graphics;
		g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
		g.setFont(DEFAULT_FONT);

		if (getIndex(g) > sortedPayments.size()) {
			return NO_SUCH_PAGE;
		}

		printHeader(g);

		if (pageIndex == 0) {
			printTitle(g);
		}

		if (getIndex(g) == sortedPayments.size()) {
			// Footer label did not fit on last page, so let's print it on a new page
			footerLabel.paint(g);
			putIndex(g, getIndex(g) + 1);
		} else {
			printPayments(g);
		}

		printFooter(g, pageIndex);

		return PAGE_EXISTS;
	}

	private void printTitle(Graphics2D g) {
		String title = user.getFullName(NamingMode.FIRST_NAME_FIRST);
		String dateString = "Stand " + DATE_FORMAT.format(new Date());

		g.setFont(TITLE_FONT);
		int yTitle = g.getFontMetrics().getAscent();
		int yText = yTitle + g.getFontMetrics().getDescent();
		g.drawString(title, 0, yTitle);

		g.setFont(DEFAULT_FONT);
		int yDate = g.getFontMetrics().getAscent() + TEXT_OFFSET_Y;
		int xDate = g.getClipBounds().width - g.getFontMetrics().stringWidth(dateString) - TEXT_OFFSET_X;
		g.drawString(dateString, xDate, yDate);

		g.translate(0, yTitle +  yText);
	}

	private void printPayments(Graphics2D g) {
		FontMetrics metrics = g.getFontMetrics();
		int textHeight = TEXT_OFFSET_Y + metrics.getAscent();
		int lineHeight = textHeight + metrics.getDescent();
		int rowHeight = textHeight + metrics.getDescent() + TEXT_OFFSET_Y;
		int neededFooterSpace = footerLabel.getHeight() + 2 * FOOTER_OFFSET;

		Rectangle clipBounds = g.getClipBounds();
		clipBounds.width = clipBounds.width - 2;
		g.setClip(clipBounds);
		g.translate(0, 2);

		int length = Math.max(metrics.stringWidth(maxLenValue.getAmountString(true)), metrics.stringWidth("Gutschrift") + 5);
		int width = g.getClipBounds().width - 3;
		int[] xDividers = new int[3];
		xDividers[0] = metrics.stringWidth(DATE_FORMAT.format(new Date(0))) + 2 * TEXT_OFFSET_X;
		xDividers[2] = width - (length + 2 * TEXT_OFFSET_X);
		xDividers[1] = xDividers[2] - (length + 2 * TEXT_OFFSET_X);
		int descriptionLength = xDividers[1] - xDividers[0] - 2 * TEXT_OFFSET_X;
		boolean first = true;

		printTableHeader(g, xDividers);
		AffineTransform tableOrigin = g.getTransform();

		int paymentNumber;
		if (onePage) {
			paymentNumber = getPaymentIndex(g, descriptionLength);
		} else {
			paymentNumber = getIndex(g);
		}

		g.setStroke(THIN_STROKE);
		for (; paymentNumber < sortedPayments.size(); ++paymentNumber) {
			Payment payment = sortedPayments.get(paymentNumber);
			MonetaryValue value = payment.getValue();
			String[] description = splitLines(payment.getDescription(), descriptionLength, metrics);
			int height = description.length * lineHeight + TEXT_OFFSET_Y;

			Rectangle userClip = g.getClipBounds();
			double ySpaceLeft = userClip.height + userClip.y;
			if (paymentNumber == sortedPayments.size() - 1) {
				// Offset for the footer
				ySpaceLeft -= rowHeight;
			}
			if (ySpaceLeft < height + rowHeight) break;

			if (first) {
				first = false;
			} else {
				g.setColor(Color.GRAY);
				g.drawLine(0, 0, width, 0);
				g.setColor(Color.BLACK);
			}

			g.translate(TEXT_OFFSET_X, textHeight);
			g.drawString(DATE_FORMAT.format(payment.getDate()), 0, 0);
			String amount = value.abs().getAmountString(false);
			if (value.signum() == -1) {
				g.drawString("Fr.", xDividers[1], 0);
				g.drawString(amount, xDividers[2] - (2 * TEXT_OFFSET_X + metrics.stringWidth(amount)), 0);
			} else {
				g.drawString("Fr.", xDividers[2], 0);
				g.drawString(amount, width - (2 * TEXT_OFFSET_X + metrics.stringWidth(amount)), 0);
			}

			for (String desc : description) {
				g.drawString(desc, xDividers[0], 0);
				g.translate(0, lineHeight);
			}

			g.translate(-TEXT_OFFSET_X, TEXT_OFFSET_Y - textHeight);
		}

		int height = (int) Math.round((tableOrigin.getTranslateY() - g.getTransform().getTranslateY()) / tableOrigin.getScaleY());
		g.setStroke(LINE_STROKE);
		for (int xDivider : xDividers) {
			g.drawLine(xDivider, 0, xDivider, height);
		}

		if (paymentNumber == sortedPayments.size()) {
			g.setClip(clipBounds.x + 2, g.getClipBounds().y, clipBounds.width, clipBounds.height);
			printTableFooter(g, xDividers);

			if (g.getClipBounds().height + g.getClipBounds().y > neededFooterSpace) {
				g.translate(5, FOOTER_OFFSET);
				footerLabel.print(g);
				g.translate(-5, footerLabel.getHeight());
				++paymentNumber;
			}
		}

		g.translate(0, g.getClipBounds().y - clipBounds.y);
		g.setClip(clipBounds);

		putIndex(g, paymentNumber);
	}

	private int getPaymentIndex(Graphics2D g, int descriptionLength) {
		FontMetrics metrics = g.getFontMetrics();
		int lineHeight = TEXT_OFFSET_Y + metrics.getAscent() + metrics.getDescent();
		int neededFooterSpace = footerLabel.getHeight() + 2 * FOOTER_OFFSET;

		int heightRemaining = g.getClipBounds().height + g.getClipBounds().y - neededFooterSpace;
		heightRemaining -= lineHeight + TEXT_OFFSET_Y; // Offset for the footer
		int paymentNumber = sortedPayments.size() - 1;
		for (; paymentNumber >= 0; --paymentNumber) {
			Payment payment = sortedPayments.get(paymentNumber);
			String[] description = splitLines(payment.getDescription(), descriptionLength, metrics);
			int height = description.length * lineHeight + TEXT_OFFSET_Y;

			if (height > heightRemaining) {
				break;
			} else {
				heightRemaining -= height;
			}
		}

		return paymentNumber + 1;
	}

	private static String[] splitLines(String original, int width, FontMetrics metrics) {
		if (original == null || original.isEmpty()) return new String[] {""};

		List<String> output = new ArrayList<>();
		String[] words = original.split(" ");
		int spacerLength = metrics.charWidth(' ');

		for (int i = 0; i < words.length;) {
			StringBuilder line = new StringBuilder(words[i++]);
			int lineLength = metrics.stringWidth(line.toString());

			while (i < words.length) {
				String word = words[i];

				lineLength += spacerLength + metrics.stringWidth(word);
				if (lineLength > width) break;

				++i;
				line.append(" ").append(word);
			}

			output.add(line.toString());
		}

		return output.toArray(new String[0]);
	}

	private void printTableHeader(Graphics2D g, int[] xDividers) {
		int yText = g.getFontMetrics().getAscent();
		int height = yText + g.getFontMetrics().getDescent() + TEXT_OFFSET_Y;
		int width = (int) g.getClipBounds().getWidth() - 3;

		g.setStroke(LINE_STROKE);
		g.drawLine(0, height, width, height);
		for (int xDivider : xDividers) {
			g.drawLine(xDivider, height, xDivider, 0);
		}

		g.translate(TEXT_OFFSET_X, yText);
		g.drawString("Datum", 0, 0);
		g.drawString("Beschreibung", xDividers[0], 0);
		g.drawString("Belastung", xDividers[1], 0);
		g.drawString("Gutschrift", xDividers[2], 0);
		g.translate(-TEXT_OFFSET_X, -yText);

		g.translate(0, height);
	}

	private void printTableFooter(Graphics2D g, int[] xDividers) {
		FontMetrics metrics = g.getFontMetrics();
		int width = g.getClipBounds().width - 3;
		int yText = TEXT_OFFSET_Y + metrics.getAscent();
		int yLow = metrics.getDescent() + TEXT_OFFSET_Y;
		int height = yText + yLow;

		g.setStroke(LINE_STROKE);
		g.drawLine(0, 0, width, 0);
		g.drawLine(xDividers[1], height, xDividers[1], 0);

		g.translate(TEXT_OFFSET_X, yText);
		g.drawString("Saldo", 0, 0);

		g.drawString("Fr.", xDividers[1], 0);
		g.translate(-TEXT_OFFSET_X, 0);

		String balance = user.getBalance().getAmountString(false);
		g.drawString(balance, width - (TEXT_OFFSET_X + metrics.stringWidth(balance)), 0);
	}

	private void printFooter(Graphics2D g, int pageIndex) {
		g.setFont(DEFAULT_FONT);
		g.setColor(Color.BLACK);
		FontMetrics metrics = g.getFontMetrics();

		Rectangle clipBounds = g.getClipBounds();
		g.translate(0, clipBounds.height + clipBounds.y - metrics.getDescent());

		g.drawString(user.getFullName(), 0, 0);

		String className = "Klasse " + studentClass.getName();
		int xMiddle = (clipBounds.width - metrics.stringWidth(className)) / 2;
		g.drawString(className, xMiddle, 0);

		String page = "Seite " + String.valueOf(pageIndex + 1);
		int xRight = clipBounds.width - metrics.stringWidth(page);
		g.drawString(page, xRight, 0);
	}
}
