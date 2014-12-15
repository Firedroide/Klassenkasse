package ch.kanti_wohlen.klassenkasse.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;

import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;

public class SearchTextField extends JTextField {

	private JLabel iconLabel;

	public SearchTextField() {
		init();
	}

	public SearchTextField(String text) {
		super(text);
		init();
	}

	public SearchTextField(int columns) {
		super(columns);
		init();
	}

	public SearchTextField(String text, int columns) {
		super(text, columns);
		init();
	}

	public SearchTextField(Document doc, String text, int columns) {
		super(doc, text, columns);
		init();
	}

	private void init() {
		iconLabel = new JLabel(IconProvider.DETAILS);
		iconLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		iconLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		iconLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				setText("");
			}
		});

		setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		setLayout(new BorderLayout());
		add(iconLabel, BorderLayout.EAST);
		getDocument().addUndoableEditListener(new UndoableEditListener() {

			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				if (getText().isEmpty()) {
					iconLabel.setIcon(IconProvider.DETAILS);
				} else {
					iconLabel.setIcon(IconProvider.DELETE);
				}
			}
		});
	}
}
