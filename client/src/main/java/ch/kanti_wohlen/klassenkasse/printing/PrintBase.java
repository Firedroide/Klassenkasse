package ch.kanti_wohlen.klassenkasse.printing;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class PrintBase {

	protected static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag("de-ch"));

	protected static final int TEXT_OFFSET_X = 8;
	protected static final int TEXT_OFFSET_Y = 3;
	protected static final int IMAGE_OFFSET_Y = 20;

	protected static final Font TITLE_FONT = newSansSerif(Font.PLAIN, 21);
	protected static final Font DEFAULT_FONT = newSansSerif(Font.PLAIN, 11);

	protected static final Stroke LINE_STROKE = new BasicStroke(1.2f);
	protected static final Stroke THIN_STROKE = new BasicStroke(0.8f);

	protected final BufferedImage headerImage;
	private final Map<Class<? extends Graphics>, Integer> indices;

	public PrintBase(BufferedImage headerImage) {
		this.headerImage = headerImage;
		indices = new HashMap<>();
	}

	private static Font newSansSerif(int style, float height) {
		Font sansSerif = Font.decode("Arial");
		if (sansSerif == null) {
			sansSerif = Font.decode(Font.SANS_SERIF);
		}

		return sansSerif.deriveFont(style, height);
	}

	protected int getIndex(Graphics g) {
		Integer i = indices.get(g.getClass());

		if (i == null) {
			indices.put(g.getClass(), 0);
			return 0;
		} else {
			return i;
		}
	}

	protected void putIndex(Graphics g, int index) {
		indices.put(g.getClass(), index);
	}

	protected void printHeader(Graphics2D g) {
		if (headerImage == null) return;

		int width = headerImage.getWidth();
		int height = headerImage.getHeight();
		int x = (g.getClipBounds().width - width) / 2;

		g.drawImage(headerImage, x, 0, null);

		g.translate(0, height + IMAGE_OFFSET_Y);
	}
}
