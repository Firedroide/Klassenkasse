package ch.kanti_wohlen.klassenkasse.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.User.NamingMode;
import ch.kanti_wohlen.klassenkasse.ui.MainWindow;
import ch.kanti_wohlen.klassenkasse.ui.components.ColorPicker;
import ch.kanti_wohlen.klassenkasse.ui.util.ConditionalFormatting;
import ch.kanti_wohlen.klassenkasse.ui.util.CurrencyFormat;
import ch.kanti_wohlen.klassenkasse.util.Configuration;
import ch.kanti_wohlen.klassenkasse.util.FileConfiguration;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class PreferencesDialog extends JDialog implements ActionListener {

	private final FileConfiguration preferences;

	private final JPanel contentPanel;
	private final JFormattedTextField classHighTextField;
	private JComboBox<String> formatComboBox;
	private ColorPicker colorHighPicker;
	private JFormattedTextField classLowTextField;
	private ColorPicker colorLowPicker;
	private ColorPicker colorMediumPicker;
	private JFormattedTextField userHighTextField;
	private JFormattedTextField userLowTextField;

	/**
	 * Create the dialog.
	 */
	public PreferencesDialog(MainWindow mainWindow) {
		this.preferences = mainWindow.getPreferences();

		setResizable(false);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Einstellungen");
		setBounds(100, 100, 403, 519);

		contentPanel = new JPanel();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new MigLayout("", "[grow,fill]", "[][]"));
		{
			JPanel userPanel = new JPanel();
			userPanel.setBorder(new TitledBorder(null, "Benutzernamen", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			contentPanel.add(userPanel, "cell 0 0,alignx left,aligny top");
			userPanel.setLayout(new MigLayout("gapx 20", "[120px][120px,grow]", "[]"));
			{
				JLabel formatLabel = new JLabel("Format");
				userPanel.add(formatLabel, "cell 0 0");
			}
			{
				formatComboBox = new JComboBox<>();
				formatComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"Vorname Nachname", "Nachname, Vorname"}));
				userPanel.add(formatComboBox, "cell 1 0,growx");
			}
		}
		{
			JPanel conditionalFormattingPanel = new JPanel();
			conditionalFormattingPanel.setBorder(new TitledBorder(null, "Bedingte Formatierung", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			contentPanel.add(conditionalFormattingPanel, "cell 0 1,grow");
			conditionalFormattingPanel.setLayout(new MigLayout("gap 20 3", "[120px][120px,grow]", "[][][][]10[][][]10[][][]"));
			{
				JLabel textColorsLabel = new JLabel("Schriftfarben");
				textColorsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
				conditionalFormattingPanel.add(textColorsLabel, "cell 0 0");
			}
			{
				JLabel colorHighLabel = new JLabel("Hohe Werte");
				conditionalFormattingPanel.add(colorHighLabel, "cell 0 1");
			}
			{
				colorHighPicker = new ColorPicker();
				conditionalFormattingPanel.add(colorHighPicker, "cell 1 1,grow");
			}
			{
				JLabel colorMediumLabel = new JLabel("Tiefe Werte");
				conditionalFormattingPanel.add(colorMediumLabel, "cell 0 2");
			}
			{
				colorMediumPicker = new ColorPicker();
				conditionalFormattingPanel.add(colorMediumPicker, "cell 1 2,grow");
			}
			{
				JLabel colorLowLabel = new JLabel("Zu tiefe Werte");
				conditionalFormattingPanel.add(colorLowLabel, "cell 0 3");
			}
			{
				colorLowPicker = new ColorPicker();
				conditionalFormattingPanel.add(colorLowPicker, "cell 1 3,grow");
			}
			{
				JLabel classLabel = new JLabel("Klassen");
				classLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
				conditionalFormattingPanel.add(classLabel, "cell 0 4");
			}
			{
				JLabel classHighLabel = new JLabel("Hohe Werte ab");
				conditionalFormattingPanel.add(classHighLabel, "cell 0 5");
			}
			{
				classHighTextField = new JFormattedTextField(CurrencyFormat.DEFAULT);
				classHighTextField.setValue(MonetaryValue.ZERO);
				classHighTextField.setHorizontalAlignment(SwingConstants.TRAILING);
				classHighTextField.addActionListener(APPLIER);
				conditionalFormattingPanel.add(classHighTextField, "cell 1 5,growx");
			}
			{
				JLabel classLowLabel = new JLabel("Zu tiefe Werte unter");
				conditionalFormattingPanel.add(classLowLabel, "cell 0 6");
			}
			{
				classLowTextField = new JFormattedTextField(CurrencyFormat.DEFAULT);
				classLowTextField.setValue(MonetaryValue.ZERO);
				classLowTextField.setHorizontalAlignment(SwingConstants.TRAILING);
				classLowTextField.addActionListener(APPLIER);
				conditionalFormattingPanel.add(classLowTextField, "cell 1 6,growx");
			}
			{
				JLabel userLabel = new JLabel("Benutzer");
				userLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
				conditionalFormattingPanel.add(userLabel, "cell 0 7");
			}
			{
				JLabel userHighLabel = new JLabel("Hohe Werte ab");
				conditionalFormattingPanel.add(userHighLabel, "cell 0 8");
			}
			{
				userHighTextField = new JFormattedTextField(CurrencyFormat.DEFAULT);
				userHighTextField.setValue(MonetaryValue.ZERO);
				userHighTextField.setHorizontalAlignment(SwingConstants.TRAILING);
				userHighTextField.addActionListener(APPLIER);
				conditionalFormattingPanel.add(userHighTextField, "cell 1 8,growx");
			}
			{
				JLabel userLowLabel = new JLabel("Zu tiefe Werte unter");
				conditionalFormattingPanel.add(userLowLabel, "cell 0 9");
			}
			{
				userLowTextField = new JFormattedTextField(CurrencyFormat.DEFAULT);
				userLowTextField.setValue(MonetaryValue.ZERO);
				userLowTextField.setHorizontalAlignment(SwingConstants.TRAILING);
				userLowTextField.addActionListener(APPLIER);
				conditionalFormattingPanel.add(userLowTextField, "cell 1 9,growx");
			}
		}
		{
			// Conditional formatting
			classHighTextField.addFocusListener(restrict(classLowTextField, true));
			classLowTextField.addFocusListener(restrict(classHighTextField, false));
			userHighTextField.addFocusListener(restrict(userLowTextField, true));
			userLowTextField.addFocusListener(restrict(userHighTextField, false));
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(this);
				buttonPane.add(cancelButton);
			}
		}

		loadData();
	}

	public void loadData() {
		String modeString = preferences.getString("userNamePreference");

		if (modeString.equals("FIRST_NAME_FIRST")) {
			formatComboBox.setSelectedIndex(0);
		} else if (modeString.equals("LAST_NAME_FIRST")) {
			formatComboBox.setSelectedIndex(1);
		}

		Configuration colors = preferences.getSubsection("monetaryValues.colors");
		colorHighPicker.setSelectedColor(new Color(colors.getInteger("high")));
		colorMediumPicker.setSelectedColor(new Color(colors.getInteger("medium")));
		colorLowPicker.setSelectedColor(new Color(colors.getInteger("low")));

		Configuration classes = preferences.getSubsection("monetaryValues.class");
		classHighTextField.setValue(new MonetaryValue(classes.getLong("high")));
		classLowTextField.setValue(new MonetaryValue(classes.getLong("low")));

		Configuration users = preferences.getSubsection("monetaryValues.user");
		userHighTextField.setValue(new MonetaryValue(users.getLong("high")));
		userLowTextField.setValue(new MonetaryValue(users.getLong("low")));
	}

	public void saveDataAndApply() {
		if (formatComboBox.getSelectedIndex() == 0) {
			preferences.set("userNamePreference", "FIRST_NAME_FIRST");
			User.setDefaultNamingMode(NamingMode.FIRST_NAME_FIRST);
		} else {
			preferences.set("userNamePreference", "LAST_NAME_FIRST");
			User.setDefaultNamingMode(NamingMode.LAST_NAME_FIRST);
		}

		Configuration colors = preferences.getSubsection("monetaryValues.colors");
		colors.set("high", colorHighPicker.getSelectedColor().getRGB());
		colors.set("medium", colorMediumPicker.getSelectedColor().getRGB());
		colors.set("low", colorLowPicker.getSelectedColor().getRGB());

		Configuration classes = preferences.getSubsection("monetaryValues.class");
		MonetaryValue classHigh = (MonetaryValue) classHighTextField.getValue();
		if (classHigh != null) classes.set("high", classHigh.getCentValue());
		MonetaryValue classLow = (MonetaryValue) classLowTextField.getValue();
		if (classLow != null) classes.set("low", classLow.getCentValue());

		Configuration users = preferences.getSubsection("monetaryValues.user");
		MonetaryValue userHigh = (MonetaryValue) userHighTextField.getValue();
		if (userHigh != null) users.set("high", userHigh.getCentValue());
		MonetaryValue userLow = (MonetaryValue) userLowTextField.getValue();
		if (userLow != null) users.set("low", userLow.getCentValue());

		ConditionalFormatting.setConditionalFormatting(preferences);

		preferences.save();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("OK")) {
			saveDataAndApply();
		}

		dispose();
	}

	private static final ActionListener APPLIER = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFormattedTextField textField = (JFormattedTextField) e.getSource();
			try {
				textField.commitEdit();
			} catch (ParseException ex) {}

			Object value = textField.getValue();
			if (value != null) {
				textField.setValue(value);
			}

			// Make sure our hack of a FocusAdapter is called to validate the value
			textField.dispatchEvent(new FocusEvent(textField, FocusEvent.FOCUS_LOST, true));
		}
	};

	private FocusAdapter restrict(final JFormattedTextField other, final boolean higher) {
		return new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				JFormattedTextField source = (JFormattedTextField) e.getSource();
				try {
					source.commitEdit();
				} catch (ParseException ex) {}

				MonetaryValue sourceValue = (MonetaryValue) source.getValue();
				MonetaryValue otherValue = (MonetaryValue) other.getValue();

				int cmp = sourceValue.compareTo(otherValue);
				if ((cmp == -1 && higher) || (cmp == 1 && !higher)) {
					source.setValue(otherValue);
				}
			}
		};
	}
}
