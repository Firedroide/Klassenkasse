package ch.kanti_wohlen.klassenkasse.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.RemoteHost;
import ch.kanti_wohlen.klassenkasse.login.LoginServerException;
import ch.kanti_wohlen.klassenkasse.ui.dialog.HostEditDialog;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.util.Configuration;
import ch.kanti_wohlen.klassenkasse.util.FileConfiguration;

import com.jgoodies.validation.Validatable;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.view.ValidationComponentUtils;

public class LoginWindow extends JFrame {

	private static final @NonNull String SERVERS_FILE_NAME = "servers.yaml";

	private final JPanel contentPanel = new JPanel();
	private final LoginDialogValidator validator;

	private JTextField usernameField;
	private JPasswordField passwordField;
	private JComboBox<String> serversComboBox;
	private JCheckBox rememberMeCheckBox;
	private JButton loginButton;

	private boolean tokenCached;
	private Host selectedHost;
	private FileConfiguration servers;
	private Set<String> hostNames;
	private JLabel infoLabel;

	/**
	 * Create the dialog.
	 */
	public LoginWindow() {
		setIconImage(IconProvider.APPLICATION.getImage());
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);
		setTitle("Server auswählen");
		setBounds(200, 150, 314, 290);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new MigLayout("hidemode 2", "[grow]", "[]0[]10[]10[][][][][]10[]"));

		validator = new LoginDialogValidator(this);
		servers = new FileConfiguration(SERVERS_FILE_NAME, false);
		hostNames = new HashSet<>();
		loadHosts();
		{
			JLabel serverLabel = new JLabel("Server:");
			contentPanel.add(serverLabel, "cell 0 0,alignx left,gapx 3");
		}
		{
			serversComboBox = new JComboBox<String>();
			serversComboBox.setModel(new DefaultComboBoxModel<>(hostNames.toArray(new String[0])));
			serversComboBox.addItemListener(new ServerSelectionListener());

			contentPanel.add(serversComboBox, "flowx,cell 0 1,growx");
		}
		{
			JSeparator separator = new JSeparator();
			contentPanel.add(separator, "cell 0 2,growx");
		}
		{
			JLabel usernameLabel = new JLabel("Benutzername");
			contentPanel.add(usernameLabel, "cell 0 3,alignx left,gapx 3");
		}
		{
			JButton editServersButton = new JButton(IconProvider.WRENCH);
			editServersButton.setAlignmentX(Component.CENTER_ALIGNMENT);
			editServersButton.setIconTextGap(0);
			editServersButton.setMargin(new Insets(0, -5, 0, -5));
			File serversFile = new File(SERVERS_FILE_NAME);
			editServersButton.setEnabled(!serversFile.exists() || serversFile.canWrite());
			editServersButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					HostEditDialog dialog = new HostEditDialog(SERVERS_FILE_NAME);
					if (serversComboBox.getModel().getSize() != 0) {
						dialog.setSelectedIndex(serversComboBox.getSelectedIndex());
					}
					dialog.setVisible(true);

					loadHosts();
					serversComboBox.setModel(new DefaultComboBoxModel<>(hostNames.toArray(new String[0])));
					usernameField.setEnabled(!hostNames.isEmpty());
					passwordField.setEnabled(!hostNames.isEmpty());
					rememberMeCheckBox.setEnabled(!hostNames.isEmpty());
				}
			});
			contentPanel.add(editServersButton, "cell 0 1");
		}
		{
			usernameField = new JTextField();
			usernameField.setEnabled(!hostNames.isEmpty());
			usernameField.addFocusListener(validator);
			usernameField.getDocument().addUndoableEditListener(validator);
			contentPanel.add(usernameField, "cell 0 4,growx");
		}
		{
			JLabel passwordLabel = new JLabel("Passwort");
			contentPanel.add(passwordLabel, "cell 0 5,gapx 3");
		}
		{
			passwordField = new JPasswordField();
			passwordField.setEnabled(!hostNames.isEmpty());
			passwordField.addFocusListener(new FocusAdapter() {

				@Override
				public void focusGained(FocusEvent e) {
					if (tokenCached) {
						passwordField.setText("");
						tokenCached = false;
					}
				}
			});
			passwordField.addFocusListener(validator);
			passwordField.getDocument().addUndoableEditListener(validator);
			contentPanel.add(passwordField, "cell 0 6,growx");
		}
		{
			rememberMeCheckBox = new JCheckBox("Login speichern");
			rememberMeCheckBox.setEnabled(!hostNames.isEmpty());
			rememberMeCheckBox.setVisible(false); // Disable for now. Implemented, but unused
			contentPanel.add(rememberMeCheckBox, "cell 0 7");
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				loginButton = new JButton("Login");
				loginButton.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						if (!loginButton.isEnabled()) return;
						loginButton.setEnabled(false);

						Object item = serversComboBox.getSelectedItem();
						if (item == null) return;

						String name = (String) item;
						Configuration serverConfig = servers.getSubsection(name);
						String host = serverConfig.getString("host");
						if (host == null) throw new NullPointerException("Host was null."); // can't happen

						int port = serverConfig.getInteger("port");
						RemoteHost remoteHost = new RemoteHost(name, host, port);
						remoteHost.start();

						char[] password = passwordField.getPassword();
						String username = usernameField.getText();
						if (password == null || username == null) {
							error("Benutzername oder Passwort fehlen.");
							return;
						}

						String token = null;
						try {
							token = remoteHost.getLoginProvider().logIn(remoteHost, username, password, tokenCached);
						} catch (LoginServerException ex) {
							if (ex.getCause() instanceof TimeoutException) {
								error("Login fehlgeschlagen: Server antwortet nicht.");
							} else {
								error("Login fehlgeschlagen.");
							}
							return;
						}

						serverConfig.set("username", username);
						if (token != null && rememberMeCheckBox.isSelected()) {
							serverConfig.set("token", token);
						}

						servers.save();
						selectedHost = remoteHost;
						end(); // Return to KlassenkassenClient
					}
				});
				loginButton.setEnabled(false);
				buttonPane.add(loginButton);
				getRootPane().setDefaultButton(loginButton);
			}
		}
		{
			ValidationComponentUtils.setMessageKey(usernameField, "username");
			ValidationComponentUtils.setMessageKey(passwordField, "password");
		}
		{
			infoLabel = new JLabel((Icon) null);
			infoLabel.setVisible(false);
			contentPanel.add(infoLabel, "cell 0 8,gapx 2");
		}

		serversComboBox.setSelectedItem(null);
		if (servers.containsKey("lastSelected")) {
			serversComboBox.setSelectedItem(servers.getString("lastSelected"));
		} else if (serversComboBox.getItemCount() > 0) {
			serversComboBox.setSelectedIndex(0);
		}
	}

	private void loadHosts() {
		servers.reload();
		hostNames.clear();
		hostNames.addAll(servers.getSubsectionKeys());
	}

	private void end() {
		char[] chars = passwordField.getPassword();
		for (int i = 0; i < chars.length; ++i) {
			chars[i] = (char) 0;
		}
		dispose();
	}

	public void error(String message) {
		infoLabel.setVisible(true);
		infoLabel.setText(message);
		infoLabel.setIcon(IconProvider.ERROR);
		loginButton.setEnabled(true);
	}

	public Host getSelectedHost() {
		return selectedHost;
	}

	private static class LoginDialogValidator implements Validatable, FocusListener, UndoableEditListener {

		private final LoginWindow parent;
		private final ValidationResultModel validationResultModel;
		private final HashSet<Component> editedComponents = new HashSet<>();

		public LoginDialogValidator(LoginWindow parent) {
			this.parent = parent;
			validationResultModel = new DefaultValidationResultModel();
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
			if (editedComponents.contains(parent.usernameField)) {
				if (parent.usernameField.getText().isEmpty()) {
					result.addError("Geben Sie Ihren Benutzernamen ein.", "username");
				}
			} else {
				allChecked = false;
			}

			if (editedComponents.contains(parent.passwordField)) {
				if (parent.passwordField.getPassword().length == 0) {
					result.addError("Geben Sie die Ihr Passwort ein.", "password");
				}
			} else {
				allChecked = false;
			}

			parent.loginButton.setEnabled(allChecked && !result.hasErrors());
			validationResultModel.setResult(result);
			return result;
		}
	}

	private class ServerSelectionListener implements ItemListener {

		private Object item;

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				if (item != e.getItem()) {
					item = e.getItem();

					String host = (String) item;
					if (servers.containsKey(host + ".username")) {
						usernameField.setText(servers.getString(host + ".username"));
						validator.editedComponents.add(usernameField);

						if (servers.containsKey(host + ".token")) {
							passwordField.setText(servers.getString(host + ".token"));
							validator.editedComponents.add(passwordField);
							rememberMeCheckBox.setSelected(true);
							tokenCached = true;
						} else {
							passwordField.setText("");
							passwordField.requestFocus();
						}

						validator.validate();
					} else {
						usernameField.setText("");
						usernameField.requestFocus();
					}

					servers.set("lastSelected", host);
				}
			}
		}
	}
}
