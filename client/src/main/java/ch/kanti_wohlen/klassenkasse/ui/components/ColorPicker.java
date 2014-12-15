package ch.kanti_wohlen.klassenkasse.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;

public class ColorPicker extends JPanel {

	private final JTextField previewField;
	private final JColorChooser colorChooser;
	private final JDialog frame;

	public ColorPicker() {
		this(Color.WHITE);
	}

	public ColorPicker(Color defaultColor) {
		setLayout(new BorderLayout(0, 0));

		colorChooser = new JColorChooser(defaultColor);
		colorChooser.setPreviewPanel(new JPanel());
		AbstractColorChooserPanel[] allPanels = colorChooser.getChooserPanels();
		List<AbstractColorChooserPanel> panels = new ArrayList<>();
		for (AbstractColorChooserPanel panel : allPanels) {
			if (panel.getDisplayName().equals("Swatches")) {
				panels.add(panel);
			} else if (panel.getDisplayName().equals("RGB")) {
				removeTransparencySlider(panel);
				panels.add(panel);
			}
		}
		colorChooser.setChooserPanels(panels.toArray(new AbstractColorChooserPanel[0]));

		previewField = new JTextField();
		previewField.setEditable(false);
		previewField.setFocusable(false);
		previewField.setColumns(10);
		previewField.setBackground(colorChooser.getColor());
		add(previewField, BorderLayout.CENTER);

		frame = new JDialog();
		frame.setTitle("Farbe wählen");
		frame.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
		frame.setContentPane(colorChooser);
		frame.setType(Window.Type.UTILITY);
		frame.pack();
		frame.setResizable(false);

		JButton popupButton = new JButton(IconProvider.COLOR_PICKER);
		popupButton.setMargin(new Insets(0, -5, 0, -5));

		popupButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(true);

				if (frame.getLocation().equals(new Point())) {
					Point screenPos = getLocationOnScreen();
					screenPos.x += 50;
					screenPos.y -= 20;
					frame.setLocation(screenPos);
				}
			}
		});

		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowDeactivated(WindowEvent e) {
				frame.setVisible(false);
				previewField.setBackground(colorChooser.getColor());
			}
		});

		add(popupButton, BorderLayout.EAST);
	}

	public Color getSelectedColor() {
		return previewField.getBackground();
	}

	public void setSelectedColor(Color color) {
		previewField.setBackground(color);
	}

	/**
	 * Hacky solution by StackOverflow user aymeric
	 */
	private static void removeTransparencySlider(AbstractColorChooserPanel cp) {
		try {
			Field f = cp.getClass().getDeclaredField("panel");
			f.setAccessible(true);

			Object colorPanel = f.get(cp);
			Field f2 = colorPanel.getClass().getDeclaredField("spinners");
			f2.setAccessible(true);
			Object spinners = f2.get(colorPanel);

			Object transpSlispinner = Array.get(spinners, 3);
			Field f3 = transpSlispinner.getClass().getDeclaredField("slider");
			f3.setAccessible(true);
			JSlider slider = (JSlider) f3.get(transpSlispinner);
			slider.setVisible(false);
			Field f4 = transpSlispinner.getClass().getDeclaredField("spinner");
			f4.setAccessible(true);
			JSpinner spinner = (JSpinner) f4.get(transpSlispinner);
			spinner.setVisible(false);
			Field f5 = transpSlispinner.getClass().getDeclaredField("label");
			f5.setAccessible(true);
			JComponent label = (JComponent) f5.get(transpSlispinner);
			label.setVisible(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
