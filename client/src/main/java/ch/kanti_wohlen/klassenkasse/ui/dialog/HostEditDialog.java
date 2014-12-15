package ch.kanti_wohlen.klassenkasse.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;
import ch.kanti_wohlen.klassenkasse.ui.util.HandshakeTester;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.util.Configuration;
import ch.kanti_wohlen.klassenkasse.util.FileConfiguration;
import ch.kanti_wohlen.klassenkasse.util.SubConfiguration;

import com.jgoodies.validation.Severity;
import com.jgoodies.validation.Validatable;
import com.jgoodies.validation.ValidationMessage;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.extras.IconFeedbackPanel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.view.ValidationComponentUtils;

public class HostEditDialog extends JDialog implements ActionListener {

	private final @NonNull FileConfiguration servers;
	private final EventList<String> hostNames;
	private final ValidationResultModel resultModel;
	private final HostValidator validator;
	private final HandshakeTester handshakeTester;

	private final JPanel contentPanel = new JPanel();
	private final JComboBox<String> serverComboBox;
	private final JTextField nameTextField;
	private final JTextField ipTextField;
	private final JFormattedTextField portTextField;
	private final JLabel resultLabel;
	private final JButton removeButton;
	private final JButton okButton;

	private String currentServer;

	/**
	 * Create the dialog.
	 */
	public HostEditDialog(@NonNull String serversFile) {
		servers = new FileConfiguration(serversFile, false);
		EventList<String> newElementList = GlazedLists.readOnlyList(GlazedLists.eventListOf("(Neuer Eintrag)"));
		hostNames = new BasicEventList<>(newElementList.getPublisher(), newElementList.getReadWriteLock());
		for (String hostName : servers.getSubsectionKeys()) {
			hostNames.add(hostName);
		}
		CompositeList<String> hostNamesPlusNew = new CompositeList<>(newElementList.getPublisher(),
				newElementList.getReadWriteLock());
		hostNamesPlusNew.addMemberList(hostNames);
		hostNamesPlusNew.addMemberList(newElementList);

		setTitle("Server bearbeiten...");
		setResizable(false);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 403, 310);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPanel.setLayout(new MigLayout("gapy 10", "[grow,fill]", "[]2[][][]2[][]15[]"));
		{
			resultLabel = new JLabel();
			handshakeTester = new HandshakeTester(resultLabel);
			contentPanel.add(resultLabel, "cell 0 6,gapx 3,aligny top");

			validator = new HostValidator(this, resultLabel);
			resultModel = validator.getResultModel();
		}
		{
			IconFeedbackPanel iconFeedbackPanel = new IconFeedbackPanel(resultModel, contentPanel);
			getContentPane().add(iconFeedbackPanel, BorderLayout.CENTER);
		}
		{
			nameTextField = new JTextField();
			nameTextField.addFocusListener(validator);
			ValidationComponentUtils.setMessageKey(nameTextField, "name");
			contentPanel.add(nameTextField, "cell 0 4,growx");
		}
		{
			JLabel nameLabel = new JLabel("Name");
			nameLabel.setLabelFor(nameTextField);
			contentPanel.add(nameLabel, "cell 0 3,gapx 3");
		}
		{
			JPanel panel = new JPanel();
			contentPanel.add(panel, "cell 0 5,grow");
			panel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill][120px,fill]", "[]2[]"));
			{
				ipTextField = new JTextField();
				ipTextField.addFocusListener(validator);
				ipTextField.getDocument().addUndoableEditListener(validator);
				ValidationComponentUtils.setMessageKey(ipTextField, "ip");
				panel.add(ipTextField, "cell 0 1");
			}
			{
				JLabel ipLabel = new JLabel("IP / Hostname");
				ipLabel.setLabelFor(ipTextField);
				panel.add(ipLabel, "cell 0 0,gapx 3");
			}
			{
				portTextField = new JFormattedTextField(PortFormat.INSTANCE);
				portTextField.addFocusListener(validator);
				portTextField.getDocument().addUndoableEditListener(validator);
				panel.add(portTextField, "cell 1 1");
			}
			{
				JLabel portLabel = new JLabel("Port");
				portLabel.setLabelFor(portTextField);
				panel.add(portLabel, "cell 1 0,gapx 3");
			}
		}
		{
			JPanel buttonPane = new JPanel();
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			buttonPane.setLayout(new MigLayout("", "[]push[][]", "[]"));
			{
				removeButton = new JButton("Entfernen");
				removeButton.setEnabled(false);
				removeButton.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						String lastServer = currentServer;
						if (lastServer == null) return;

						serverComboBox.setSelectedIndex(serverComboBox.getItemCount() - 1);

						hostNames.remove(lastServer);
						servers.remove(lastServer);

						validator.reset();
					}
				});
				removeButton.setIcon(IconProvider.DELETE);
				buttonPane.add(removeButton, "cell 0 0,alignx left,aligny top");
			}
			{
				okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				buttonPane.add(okButton, "cell 1 0,alignx left,aligny top");
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Abbrechen");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(this);
				buttonPane.add(cancelButton, "cell 2 0,alignx left,aligny top");
			}
		}
		{
			serverComboBox = new JComboBox<String>();
			serverComboBox.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						if (serverComboBox.getSelectedIndex() == serverComboBox.getItemCount() - 1) {
							nameTextField.setText("");
							ipTextField.setText("");
							portTextField.setValue(57065);
							currentServer = null;
							removeButton.setEnabled(false);

							validator.reset();
						} else {
							String item = (String) e.getItem();
							currentServer = item;
							nameTextField.setText(item);
							ipTextField.setText(servers.getString(item + ".host"));
							portTextField.setText(String.valueOf(servers.getInteger(item + ".port")));
							removeButton.setEnabled(true);

							validator.reset();
							validator.editedComponents.add(nameTextField);
							validator.editedComponents.add(ipTextField);
							validator.validate();
						}
					}
				}
			});
			@SuppressWarnings("unchecked")
			ComboBoxModel<String> model = new DefaultEventComboBoxModel<String>(hostNamesPlusNew);
			serverComboBox.setModel(model);
			serverComboBox.setSelectedIndex(serverComboBox.getItemCount() - 1);
			contentPanel.add(serverComboBox, "cell 0 1,growx");
		}
		{
			JLabel serverLabel = new JLabel("Server");
			serverLabel.setLabelFor(serverComboBox);
			contentPanel.add(serverLabel, "cell 0 0,gapx 3");
		}
		{
			JSeparator separator = new JSeparator();
			contentPanel.add(separator, "cell 0 2");
		}
		{
			ValidationComponentUtils.setMessageKey(nameTextField, "name");
			ValidationComponentUtils.setMessageKey(ipTextField, "ip");
			ValidationComponentUtils.setMessageKey(portTextField, "port");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ("OK".equals(e.getActionCommand())) {
			if (validator.validate().hasErrors()) {
				okButton.setEnabled(false);
				return;
			}

			servers.save();
			dispose();
		} else if ("Cancel".equals(e.getActionCommand())) {
			dispose();
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		handshakeTester.cancel();
	}

	public void setSelectedIndex(int selectedIndex) {
		serverComboBox.setSelectedIndex(selectedIndex);
	}

	private void updateCurrentValue() {
		String oldName = currentServer;
		String newName = nameTextField.getText();
		if (newName == null) return; // Checked

		if (!newName.equals(oldName)) {
			if (oldName != null) {
				hostNames.remove(oldName);
				servers.remove(oldName);
			}
			hostNames.add(newName);
		}

		Configuration server = new SubConfiguration(servers, newName);
		server.set("host", ipTextField.getText());
		server.set("port", portTextField.getValue());

		removeButton.setEnabled(true);
		currentServer = newName;
		serverComboBox.setSelectedItem(currentServer);

		handshakeTester.setTarget(ipTextField.getText(), Integer.valueOf(portTextField.getText()));
	}

	private static final class PortFormat extends Format {

		public static final PortFormat INSTANCE = new PortFormat();

		private PortFormat() {}

		private static final int LOWER_BOUND = 1;
		private static final int UPPER_BOUND = 65535;
		private static final Pattern pattern = Pattern.compile("\\d{1,5}");

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			if (!(obj instanceof Integer)) return toAppendTo;

			Integer i = (Integer) obj;
			if (i < LOWER_BOUND || i > UPPER_BOUND) return toAppendTo;

			pos.setBeginIndex(toAppendTo.length());
			toAppendTo.append(i.toString());
			pos.setEndIndex(toAppendTo.length());
			return toAppendTo;
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			Matcher matcher = pattern.matcher(source);
			if (!matcher.find(pos.getIndex())) {
				pos.setErrorIndex(pos.getIndex());
				return null;
			}

			Integer i = Integer.parseInt(matcher.group());
			if (i < LOWER_BOUND || i > UPPER_BOUND) {
				pos.setErrorIndex(pos.getIndex());
				return null;
			}

			pos.setIndex(matcher.end());
			return i;
		}
	}

	private static class HostValidator implements Validatable, FocusListener, UndoableEditListener {

		private final HostEditDialog parent;
		private final JLabel resultLabel;
		private final ValidationResultModel validationResultModel;
		private final HashSet<Component> editedComponents = new HashSet<>();

		public HostValidator(HostEditDialog parent, JLabel resultLabel) {
			validationResultModel = new DefaultValidationResultModel();
			this.parent = parent;
			this.resultLabel = resultLabel;

			addStartHint();
		}

		public void reset() {
			if (validate().hasErrors()) {
				validationResultModel.setResult(new ValidationResult());
				addStartHint();
			}
			parent.handshakeTester.cancel();
			editedComponents.clear();
		}

		private void addStartHint() {
			resultLabel.setIcon(IconProvider.INFO);
			resultLabel.setText("Geben Sie die Serverparameter ein.");
		}

		public ValidationResultModel getResultModel() {
			return this.validationResultModel;
		}

		@Override
		public void focusGained(FocusEvent e) {
			editedComponents.add(e.getComponent());
		}

		@Override
		public void focusLost(FocusEvent e) {
			validate();
		}

		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			validate();
		}

		@Override
		public ValidationResult validate() {
			ValidationResult result = new ValidationResult();

			boolean allChecked = true;
			if (editedComponents.contains(parent.nameTextField)) {
				String name = parent.nameTextField.getText();
				if (name.isEmpty()) {
					result.addError("Geben Sie einen Namen für den Server ein.", "name");
				} else if (!name.equals(parent.currentServer) && parent.hostNames.contains(name)) {
					result.addError("Dieser Name existiert bereits.", "name");
				}
			} else {
				allChecked = false;
			}

			if (editedComponents.contains(parent.ipTextField)) {
				String text = parent.ipTextField.getText();
				if (text.isEmpty()) {
					result.addError("Geben Sie die IP bzw. den Hostnamen des Servers ein.", "ip");
				} else if (Pattern.compile("[^A-Za-z0-9\\._/\\-]+").matcher(text).find()) {
					result.addError("Die IP / der Hostname enthält ungültige Zeichen.", "ip");
				}
			} else {
				allChecked = false;
			}

			try {
				Integer port = Integer.valueOf(parent.portTextField.getText());
				if (port < 1 || port > 65536) {
					result.addError("Der Port muss zwischen 1 und 65536 liegen.", "port");
				}
			} catch (NumberFormatException nfe) {
				result.addError("Der Port muss eine Zahl zwischen 1 und 65536 sein.", "port");
			}

			parent.okButton.setEnabled(allChecked && !result.hasErrors());
			if (!result.hasErrors() && allChecked) {
				parent.updateCurrentValue();
			}

			validationResultModel.setResult(result);

			if (result.hasErrors()) {
				parent.handshakeTester.cancel();
			}
			updateResultLabel(result);
			return result;
		}

		// Copied from com.jgoodies.validation.view.ValidationResultViewFactory.LabelTextChangeHandler#updateText,
		// slightly modified
		private void updateResultLabel(ValidationResult result) {
			if (result.isEmpty()) {
				resultLabel.setIcon(null);
				resultLabel.setText("");
				return;
			}

			Severity severity = result.getSeverity();
			for (ValidationMessage message : result) {
				if (message.severity() == severity) {
					resultLabel.setText(message.formattedText());
					resultLabel.setIcon(IconProvider.forValidationResult(result));
					return;
				}
			}
		}
	}
}
