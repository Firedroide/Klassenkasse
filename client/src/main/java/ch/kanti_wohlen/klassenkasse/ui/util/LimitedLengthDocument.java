package ch.kanti_wohlen.klassenkasse.ui.util;

import java.awt.Toolkit;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class LimitedLengthDocument extends PlainDocument {

	private final int limit;

	public LimitedLengthDocument(int limit) {
		super();
		this.limit = limit;
	}

	public LimitedLengthDocument(int limit, Content content) {
		super(content);
		this.limit = limit;
	}

	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
		if (str == null) return;

		if (getLength() == limit) {
			// Length limit reached
			Toolkit.getDefaultToolkit().beep();
		} else if (getLength() + str.length() > limit) {
			// Not all of the content fits
			super.insertString(offs, str.substring(0, limit - getLength()), a);
			Toolkit.getDefaultToolkit().beep();
		} else {
			// All of the content fits
			super.insertString(offs, str, a);
		}
	}
}
