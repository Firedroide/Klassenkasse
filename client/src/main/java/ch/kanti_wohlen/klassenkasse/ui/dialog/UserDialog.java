package ch.kanti_wohlen.klassenkasse.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.ConstraintParser;
import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;
import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserCreated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserRemoved;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserUpdated;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.login.LoginServerException;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.ui.util.LimitedLengthDocument;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.RoleComparator;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.StudentClassComparator;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import ch.kanti_wohlen.klassenkasse.util.PermissionsHelper;

import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.Validator;
import com.jgoodies.validation.extras.IconFeedbackPanel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.view.ValidationComponentUtils;
import com.jgoodies.validation.view.ValidationResultViewFactory;

public class UserDialog extends JDialog implements ActionListener {

	private final @NonNull Host host;
	private User editingUser;

	private JComboBox<String> usernameComboBox;
	private JComboBox<String> roleComboBox;
	private JComboBox<String> classComboBox;
	private JLabel validationResultLabel;
	private JLabel usernameLabel;
	private JLabel classLabel;
	private JLabel firstNameLabel;
	private JLabel lastNameLabel;
	private JLabel roleLabel;
	private JPanel buttonPane;
	private JButton okButton;
	private JButton cancelButton;
	private JButton deleteButton;

	private final UserValidator validator;
	private final IconFeedbackPanel iconPanel;
	private final JPanel contentPanel;
	private final EventList<String> usernames;
	private final EventList<Role> userRoles;
	private final EventList<StudentClass> studentClasses;
	private final JTextField firstNameTextField;
	private final JTextField lastNameTextField;

	private @NonNull Action[] createdActions;
	private StudentClass selectedClass;
	private boolean predefinedName;

	/**
	 * Create the dialog.
	 */
	public UserDialog(@NonNull Host host) {
		this.host = host;
		validator = new UserValidator(this);

		// DATA
		createdActions = new Action[0];
		EventList<User> existingUsers = new BasicEventList<User>();
		existingUsers.addAll(host.getUsers().values());
		EventList<String> existingUsernames = GlazedLists.transformByFunction(existingUsers,
				GlazedLists.toStringFunction(User.class, "username"));

		usernames = new SortedList<>(new BasicEventList<String>());
		try {
			usernames.addAll(host.getLoginProvider().getUsernames());
		} catch (LoginServerException e) {
			JOptionPane.showMessageDialog(null, "Die Benutzernamen konnten nicht geladen werden",
					"Serverfehler", JOptionPane.ERROR_MESSAGE);
		}
		usernames.removeAll(existingUsernames);

		User loggedInUser = host.getLoggedInUser();
		if (loggedInUser == null) throw new IllegalStateException("Must be logged in.");

		userRoles = new BasicEventList<Role>();
		Map<Integer, Role> roles = host.getRoles();
		for (Entry<Integer, Role> entry : roles.entrySet()) {
			if (entry.getKey() > 0 && entry.getKey() >= loggedInUser.getRoleId()) {
				userRoles.add(entry.getValue());
			}
		}

		Collections.sort(userRoles, new RoleComparator());
		Collections.reverse(userRoles);
		final TransformedList<Role, String> strUserRoles = GlazedLists.transformByFunction(userRoles,
				GlazedLists.toStringFunction(Role.class, "name"));

		studentClasses = new BasicEventList<StudentClass>();
		studentClasses.addAll(host.getClasses().values());
		Collections.sort(studentClasses, new StudentClassComparator());
		final TransformedList<StudentClass, String> strStudentClasses = GlazedLists.transformByFunction(studentClasses,
				GlazedLists.toStringFunction(StudentClass.class, "name"));

		// LAYOUT
		setTitle("Nutzer hinzufügen");
		setIconImage(IconProvider.USER.getImage());
		setResizable(false);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setBounds(100, 100, 452, 289);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());

		contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPanel.setLayout(new MigLayout("gap 10 4", "[119.00,left][grow,fill]", "[]15[][][][]10[]"));

		iconPanel = new IconFeedbackPanel(validator.getResultModel(), contentPanel);
		getContentPane().add(iconPanel, BorderLayout.CENTER);
		{
			usernameComboBox = new JComboBox<String>();
			usernameComboBox.setEditable(true);
			usernameComboBox.setPrototypeDisplayValue(new String(new char[28]).replace("\0", "x"));
			AutoCompleteSupport.install(usernameComboBox, usernames).setFilterMode(TextMatcherEditor.CONTAINS);
			usernameComboBox.addFocusListener(validator);
			usernameComboBox.addActionListener(validator);
			contentPanel.add(usernameComboBox, "cell 1 0,growx");
		}
		{
			usernameLabel = new JLabel("Benutzername");
			usernameLabel.setHorizontalAlignment(SwingConstants.CENTER);
			usernameLabel.setLabelFor(usernameComboBox);
			contentPanel.add(usernameLabel, "cell 0 0,alignx left");
		}
		{
			roleComboBox = new JComboBox<String>();
			roleComboBox.setEnabled(false);
			@SuppressWarnings("unchecked")
			ComboBoxModel<String> model = new DefaultEventComboBoxModel<String>(strUserRoles);
			roleComboBox.setModel(model);
			roleComboBox.addActionListener(validator);
			roleComboBox.addFocusListener(validator);
			contentPanel.add(roleComboBox, "cell 1 1,growx");
		}
		{
			roleLabel = new JLabel("Nutzerrolle");
			roleLabel.setEnabled(false);
			roleLabel.setLabelFor(roleComboBox);
			contentPanel.add(roleLabel, "cell 0 1,alignx left");
		}
		{
			classComboBox = new JComboBox<>();
			classComboBox.setEnabled(false);
			classComboBox.setEditable(true);
			AutoCompleteSupport.install(classComboBox, strStudentClasses).setFilterMode(TextMatcherEditor.CONTAINS);
			classComboBox.addActionListener(validator);
			classComboBox.addFocusListener(validator);
			contentPanel.add(classComboBox, "cell 1 2,growx");
		}
		{
			classLabel = new JLabel("Klasse");
			classLabel.setEnabled(false);
			classLabel.setLabelFor(classComboBox);
			contentPanel.add(classLabel, "cell 0 2,alignx left");
		}
		{
			Document firstNameDocument = new LimitedLengthDocument(32);
			firstNameDocument.addUndoableEditListener(new UndoableEditListener() {

				@Override
				public void undoableEditHappened(UndoableEditEvent e) {
					validator.validateComponent(firstNameTextField);
				}
			});
			firstNameTextField = new JTextField(firstNameDocument, "", 32);
			firstNameTextField.setEnabled(false);
			firstNameTextField.addFocusListener(validator);
			contentPanel.add(firstNameTextField, "cell 1 3,growx");
		}
		{
			firstNameLabel = new JLabel("Vorname");
			firstNameLabel.setEnabled(false);
			firstNameLabel.setLabelFor(firstNameTextField);
			contentPanel.add(firstNameLabel, "cell 0 3,alignx left");
		}
		{
			Document lastNameDocument = new LimitedLengthDocument(32);
			lastNameDocument.addUndoableEditListener(new UndoableEditListener() {

				@Override
				public void undoableEditHappened(UndoableEditEvent e) {
					validator.validateComponent(lastNameTextField);
				}
			});
			lastNameTextField = new JTextField(lastNameDocument, "", 32);
			lastNameTextField.setEnabled(false);
			lastNameTextField.addFocusListener(validator);
			contentPanel.add(lastNameTextField, "cell 1 4,growx");
		}
		{
			lastNameLabel = new JLabel("Nachname");
			lastNameLabel.setEnabled(false);
			lastNameLabel.setLabelFor(lastNameTextField);
			contentPanel.add(lastNameLabel, "cell 0 4,alignx left");
		}
		{
			ValidationComponentUtils.setMessageKey(usernameComboBox, "username");
			ValidationComponentUtils.setMessageKey(roleComboBox, "role");
			ValidationComponentUtils.setMessageKey(classComboBox, "class");
			ValidationComponentUtils.setMessageKey(firstNameTextField, "firstName");
			ValidationComponentUtils.setMessageKey(lastNameTextField, "lastName");

			validationResultLabel = ValidationResultViewFactory.createReportIconAndTextLabel(validator.getResultModel());
			contentPanel.add(validationResultLabel, "cell 0 5,spanx 2");
		}
		{
			buttonPane = new JPanel();
			buttonPane.setLayout(new MigLayout("", "[]push[][]", "[28px]"));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				deleteButton = new JButton("Löschen");
				deleteButton.setIcon(IconProvider.DELETE);
				deleteButton.setActionCommand("Delete");
				deleteButton.addActionListener(this);
				deleteButton.setVisible(false);
				buttonPane.add(deleteButton, "cell 0 0");
			}
			{
				okButton = new JButton("OK");
				okButton.setEnabled(false);
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				okButton.grabFocus();
				buttonPane.add(okButton, "cell 1 0,alignx left,aligny top");
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton = new JButton("Abbrechen");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(this);
				buttonPane.add(cancelButton, "cell 2 0,alignx left,aligny top");
			}
		}
	}

	public void setData(@NonNull User user) {
		editingUser = user;

		usernameComboBox.getEditor().setItem(user.getUsername());
		usernameComboBox.setEnabled(false);

		int roleIndex = userRoles.indexOf(user.getRole(host));
		if (roleIndex < 0) throw new IllegalStateException("Role index was -1.");
		roleComboBox.setSelectedIndex(roleIndex);

		if (selectedClass == null) {
			int classIndex = -1;
			for (int i = 0; i < studentClasses.size(); ++i) {
				if (studentClasses.get(i).getLocalId() == user.getStudentClassId()) {
					classIndex = i;
				}
			}
			if (classIndex < 0) throw new IllegalStateException("User class was not in the list of all classes");
			classComboBox.setSelectedIndex(classIndex);
		}

		firstNameTextField.setText(user.getFirstName());
		lastNameTextField.setText(user.getLastName());
		deleteButton.setVisible(PermissionsHelper.canUpdateUser(host, user, UpdateType.REMOVAL));

		setTitle("Nutzer bearbeiten");
	}

	public void setName(String firstName, String lastName) {
		firstNameTextField.setText(firstName);
		lastNameTextField.setText(lastName);
		predefinedName = true;
	}

	public void filterUsernames(Collection<String> usernames) {
		this.usernames.removeAll(usernames);
		validator.validationResultModel.setResult(new ValidationResult());
	}

	public void setStudentClass(@NonNull StudentClass studentClass, boolean lock) {
		classComboBox.setSelectedItem(studentClass.getName());

		if (lock) {
			selectedClass = studentClass;
			classComboBox.setEnabled(false);
		}

		// Reset validation result, we don't want this to trigger yet
		validator.validationResultModel.setResult(new ValidationResult());
	}

	public String getSelectedUsername() {
		if (!usernameComboBox.isEnabled()) return editingUser.getUsername();
		if (usernameComboBox.getSelectedIndex() < 0) return null;
		return usernames.get(usernameComboBox.getSelectedIndex());
	}

	public Role getSelectedUserRole() {
		if (roleComboBox.getSelectedIndex() < 0) return null;
		return userRoles.get(roleComboBox.getSelectedIndex());
	}

	public StudentClass getSelectedStudentClass() {
		if (selectedClass != null) return selectedClass;

		if (classComboBox.getSelectedIndex() < 0) return null;
		return studentClasses.get(classComboBox.getSelectedIndex());
	}

	public String getFirstName() {
		return firstNameTextField.getText();
	}

	public String getLastName() {
		return lastNameTextField.getText();
	}

	private @NonNull User createUser() {
		String username = checkValidity(getSelectedUsername());
		Role role = checkValidity(getSelectedUserRole());

		StudentClass studentClass = null;
		studentClass = checkValidity(getSelectedStudentClass());

		String firstName = checkValidity(getFirstName());
		String lastName = checkValidity(getLastName());

		if (editingUser == null) {
			return new User(host, studentClass.getLocalId(), role.getLocalId(), firstName, lastName, username);
		} else {
			return new User(editingUser.getLocalId(), studentClass.getLocalId(), role.getLocalId(), firstName, lastName, username, editingUser.getBalance());
		}
	}

	private @NonNull Action createAction() {
		User newUser = createUser();

		if (editingUser == null) {
			return new ActionUserCreated(newUser);
		} else {
			return new ActionUserUpdated(newUser);
		}
	}

	public @NonNull Action[] getCreatedActions() {
		return createdActions;
	}

	private @NonNull <T> T checkValidity(T o) {
		if (o == null) throw new IllegalStateException();
		if (o instanceof String && ((String) o).isEmpty()) throw new IllegalStateException();
		return o;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ("OK".equals(e.getActionCommand())) {
			createdActions = new Action[] {createAction()};
		} else if ("Delete".equals(e.getActionCommand())) {
			User editingUser = checkValidity(this.editingUser);

			if (!editingUser.getBalance().equals(MonetaryValue.ZERO)) {
				if (!Dialogs.confirmUserDeletion(editingUser)) return;
			}
			createdActions = new Action[] {new ActionUserRemoved(editingUser)};
		}
		dispose();
	}

	@Override
	public void dispose() {
		usernames.dispose();
		userRoles.dispose();
		studentClasses.dispose();
		super.dispose();
	}

	private static class UserValidator extends FocusAdapter implements ActionListener, Validator<UserDialog> {

		private final ValidationResultModel validationResultModel;
		private final UserDialog parent;
		private final HashSet<Component> editedComponents = new HashSet<>();
		private String username = null;

		public UserValidator(UserDialog userDialog) {
			validationResultModel = new DefaultValidationResultModel();
			parent = userDialog;
		}

		public ValidationResultModel getResultModel() {
			return validationResultModel;
		}

		public void validateComponent(Component component) {
			editedComponents.add(component);
			validate(parent);
		}

		@Override
		public void focusLost(FocusEvent e) {
			validateComponent(e.getComponent());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			validateComponent((Component) e.getSource());
		}

		@Override
		public ValidationResult validate(UserDialog validationTarget) {
			ValidationResult result = new ValidationResult();

			if (validationTarget.editingUser == null) {
				if (validationTarget.usernameComboBox.getSelectedIndex() < 0) {
					result.addError("Geben Sie einen korrekten, bestehenden Benutzernamen ein.", "username");
				} else {
					String selectedUsername = validationTarget.getSelectedUsername();

					// Valid username that has changed
					// -> Try to fill in the first and last name
					if (username != selectedUsername && !validationTarget.predefinedName) {
						username = selectedUsername;
						Matcher matcher = Pattern.compile("([\\w\\.-]+)[^a-zA-Z0-9@]([\\w\\.-]+)").matcher(username);
						if (matcher.find()) {
							String firstName = matcher.group(1).replace('.', ' ').replace('_', ' ');
							String lastName = matcher.group(2).replace('.', ' ').replace('_', ' ');
							validationTarget.firstNameTextField.setText(capitalize(firstName));
							validationTarget.lastNameTextField.setText(capitalize(lastName));
						} else {
							validationTarget.firstNameTextField.setText("");
							validationTarget.lastNameTextField.setText("");
						}
					}

					if (validationTarget.roleComboBox.getSelectedIndex() < 0) {
						validationTarget.roleComboBox.setSelectedIndex(0);
					}
				}
			}

			boolean usernameValid = !result.hasErrors();
			MigLayout layout = (MigLayout) validationTarget.contentPanel.getLayout();
			for (Component component : validationTarget.contentPanel.getComponents()) {
				CC constraints = ConstraintParser.parseComponentConstraint(String.valueOf(layout.getComponentConstraints(component)));

				if (constraints.getCellY() == 0 || constraints.getCellY() == 5) continue;
				if (constraints.getCellY() == 2 && validationTarget.selectedClass != null) continue;

				layout.getComponentConstraints(component);
				component.setEnabled(usernameValid);
			}

			boolean allChecked = true;
			if (editedComponents.contains(validationTarget.roleComboBox)) {
				if (validationTarget.roleComboBox.getSelectedIndex() < 0) {
					result.addError("Wählen Sie eine Nutzerrolle aus.", "role");
				}
			} else {
				allChecked = false;
			}

			if (editedComponents.contains(validationTarget.classComboBox)) {
				if (validationTarget.getSelectedStudentClass() == null) {
					result.addError("Wählen Sie eine bestehende Klasse aus.", "class");
				}
			} else {
				allChecked = false;
			}

			if (editedComponents.contains(validationTarget.firstNameTextField)) {
				if (validationTarget.firstNameTextField.getText().isEmpty()) {
					result.addError("Der Vorname darf nicht leer sein.", "firstName");
				}
			} else {
				allChecked = false;
			}

			if (editedComponents.contains(validationTarget.lastNameTextField)) {
				if (validationTarget.lastNameTextField.getText().isEmpty()) {
					result.addError("Der Nachname darf nicht leer sein.", "lastName");
				}
			} else {
				allChecked = false;
			}

			validationTarget.okButton.setEnabled(allChecked && !result.hasErrors());

			validationResultModel.setResult(result);
			return result;
		}

		private static String capitalize(String value) {
			StringBuilder sb = new StringBuilder(value.replaceAll("\\.", " "));
			Matcher matcher = Pattern.compile("\\b\\w").matcher(value);
			while (matcher.find()) {
				sb.replace(matcher.start(), matcher.end(), matcher.group().toUpperCase());
			}
			return sb.toString();
		}
	}
}
