package ch.kanti_wohlen.klassenkasse.printing;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.UserComparator;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class StudentClassPrinter extends PrintBase implements Printable {

	private final StudentClass studentClass;
	private final List<User> sortedUsers;
	private final MonetaryValue maxLenValue;

	public StudentClassPrinter(Host host, StudentClass studentClass, BufferedImage headerImage) {
		super(headerImage);

		this.studentClass = studentClass;

		Collection<User> users = host.getUsersByClass(studentClass.getLocalId()).values();
		sortedUsers = new ArrayList<>(users);
		Collections.sort(sortedUsers, new UserComparator());

		MonetaryValue min = MonetaryValue.ZERO;
		for (User user : users) {
			MonetaryValue negative = user.getBalance();
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

		if (getIndex(g) >= sortedUsers.size()) {
			return NO_SUCH_PAGE;
		}

		printHeader(g);

		if (pageIndex == 0) {
			printTitle(g);
		}

		printUsers(g);
		printFooter(g, pageIndex);

		return PAGE_EXISTS;
	}

	private void printTitle(Graphics2D g) {
		String title = "Klasse " + studentClass.getName();
		String dateString = "Stand " + DATE_FORMAT.format(new Date());

		g.setFont(TITLE_FONT);
		int yTitle = g.getFontMetrics().getAscent();
		int yText = yTitle + g.getFontMetrics().getDescent();
		g.drawString(title, 0, yTitle);

		g.setFont(DEFAULT_FONT);
		int yDate = g.getFontMetrics().getAscent() + TEXT_OFFSET_Y;
		int xDate = g.getClipBounds().width - g.getFontMetrics().stringWidth(dateString) - TEXT_OFFSET_X;
		g.drawString(dateString, xDate, yDate);

		g.translate(0, 2 * yText);
	}

	private void printUsers(Graphics2D g) {
		FontMetrics metrics = g.getFontMetrics();
		int userNumber = getIndex(g);
		int textHeight = TEXT_OFFSET_Y + metrics.getAscent();
		int rowHeight = textHeight + metrics.getDescent() + TEXT_OFFSET_Y;

		Rectangle clipBounds = g.getClipBounds();
		clipBounds.width = clipBounds.width - 2;
		g.setClip(clipBounds);
		g.translate(0, 2);

		int length = Math.max(metrics.stringWidth(maxLenValue.getAmountString(true)), metrics.stringWidth("Kontostand") + 25);
		int width = g.getClipBounds().width - 3;
		int xDivider = width - (length + 2 * TEXT_OFFSET_X);
		boolean first = true;

		printTableHeader(g, xDivider);
		AffineTransform tableOrigin = g.getTransform();

		g.setStroke(THIN_STROKE);
		for (; userNumber < sortedUsers.size(); ++userNumber) {
			Rectangle userClip = g.getClipBounds();
			double ySpaceLeft = userClip.height + userClip.y;
			if (sortedUsers.size() - userNumber == 1) ySpaceLeft -= 2 * rowHeight;
			if (ySpaceLeft < 2 * rowHeight) break;

			if (first) {
				first = false;
			} else {
				g.setColor(Color.GRAY);
				g.drawLine(0, 0, width, 0);
				g.setColor(Color.BLACK);
			}

			User user = sortedUsers.get(userNumber);

			g.translate(TEXT_OFFSET_X, textHeight);
			g.drawString(user.getFullName(), 0, 0);
			g.drawString("Fr.", xDivider, 0);
			String amount = user.getBalance().getAmountString(false);
			g.drawString(amount, width - (2 * TEXT_OFFSET_X + metrics.stringWidth(amount)), 0);
			g.translate(-TEXT_OFFSET_X, -textHeight);

			g.translate(0, rowHeight);
		}
		putIndex(g, userNumber);

		int height = (int) Math.round((tableOrigin.getTranslateY() - g.getTransform().getTranslateY()) / tableOrigin.getScaleY());
		g.setStroke(LINE_STROKE);
		g.drawLine(xDivider, 0, xDivider, height);
		//g.drawRect(0, 0, width, height);

		if (userNumber == sortedUsers.size()) {
			printTableFooter(g, xDivider);
		}

		g.translate(0, g.getClipBounds().y - clipBounds.y);
		g.setClip(clipBounds);
	}

	private void printTableHeader(Graphics2D g, int xDivider) {
		int yText = g.getFontMetrics().getAscent();
		int height = yText + g.getFontMetrics().getDescent() + TEXT_OFFSET_Y;
		int width = (int) g.getClipBounds().getWidth() - 3;

		g.setStroke(LINE_STROKE);
		g.drawLine(0, height, width, height);
		g.drawLine(xDivider, height, xDivider, 0);
		//g.drawRect(0, 0, width, height);

		g.translate(TEXT_OFFSET_X, yText);
		g.drawString("Name", 0, 0);
		g.drawString("Kontostand", xDivider, 0);
		g.translate(-TEXT_OFFSET_X, -yText);

		g.translate(0, height);
	}

	private void printTableFooter(Graphics2D g, int xDivider) {
		FontMetrics metrics = g.getFontMetrics();
		int width = g.getClipBounds().width - 3;
		int yText = TEXT_OFFSET_Y + metrics.getAscent();
		int yLow = metrics.getDescent() + TEXT_OFFSET_Y;
		int height = yText + yLow;

		// Roundings
		g.drawLine(xDivider, height, xDivider, 0);
		g.setStroke(THIN_STROKE);
		g.drawLine(0, 0, width, 0);

		g.translate(TEXT_OFFSET_X, yText);
		g.drawString("Rundungen", 0, 0);
		g.drawString("Fr.", xDivider, 0);
		g.translate(-TEXT_OFFSET_X, 0);

		String rounding = studentClass.getRoundingValue().getAmountString(false);
		g.drawString(rounding, width - (TEXT_OFFSET_X + metrics.stringWidth(rounding)), 0);
		g.translate(0, yLow);

		// Balance
		g.setStroke(LINE_STROKE);
		g.drawLine(0, 0, width, 0);
		g.drawLine(xDivider, height, xDivider, 0);

		g.translate(TEXT_OFFSET_X, yText);
		g.drawString("Klassentotal", 0, 0);
		g.drawString("Fr.", xDivider, 0);
		g.translate(-TEXT_OFFSET_X, 0);

		String balance = studentClass.getBalance().getAmountString(false);
		g.drawString(balance, width - (TEXT_OFFSET_X + metrics.stringWidth(balance)), 0);
	}

	private void printFooter(Graphics2D g, int pageIndex) {
		g.setFont(DEFAULT_FONT);
		g.setColor(Color.BLACK);
		FontMetrics metrics = g.getFontMetrics();

		Rectangle clipBounds = g.getClipBounds();
		g.translate(0, clipBounds.height + clipBounds.y - metrics.getDescent());

		g.drawString("Klasse " + studentClass.getName(), 0, 0);

		String page = "Seite " + String.valueOf(pageIndex + 1);
		int x = clipBounds.width - metrics.stringWidth(page);
		g.drawString(page, x, 0);
	}
}
