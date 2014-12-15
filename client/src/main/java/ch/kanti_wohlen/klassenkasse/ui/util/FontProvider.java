package ch.kanti_wohlen.klassenkasse.ui.util;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.UIManager;

public final class FontProvider {

	public static final Font TITLE_FONT = underline(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
	public static final Font SUBTITLE_FONT = underline(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
	public static final Font DEFAULT_FONT = UIManager.getDefaults().getFont("TabbedPane.font");
	public static final Font DEFAULT_BOLD = DEFAULT_FONT.deriveFont(Font.BOLD);

	private static final Font underline(Font origin) {
		Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		return origin.deriveFont(attributes);
	}

	private FontProvider() {}
}
