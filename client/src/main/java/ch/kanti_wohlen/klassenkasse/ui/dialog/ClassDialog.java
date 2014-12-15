package ch.kanti_wohlen.klassenkasse.ui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassCreated;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassRemoved;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassUpdated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUser;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserCreated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserRemoved;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserUpdated;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.components.UserTablePanel;
import ch.kanti_wohlen.klassenkasse.ui.components.UserTablePanel.RoleMode;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.ui.util.LimitedLengthDocument;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import ch.kanti_wohlen.klassenkasse.util.PermissionsHelper;

import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.Validator;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.view.ValidationResultViewFactory;

public class ClassDialog extends JDialog implements ActionListener {

	private final @NonNull Host host;

	private final @NonNull Map<Integer, User> originalUsers;
	private final @NonNull Map<Integer, User> newUsers;
	private final @NonNull Map<Integer, ActionUser> userActions;

	private final JPanel namePanel;
	private final JTextField nameField;
	private final UserTablePanel userTable;
	private final JButton deleteButton;
	private final JButton okButton;

	private @NonNull Action[] createdActions;
	private @NonNull StudentClass editingClass;
	private boolean isEdit;
	private String originalName;

	/**
	 * Create the dialog.
	 */
	@SuppressWarnings("serial")
	public ClassDialog(final @NonNull Host host) {
		this.host = host;
		createdActions = new Action[0];
		editingClass = new StudentClass(host, "(Neue Klasse)");
		isEdit = false;
		originalName = null;

		originalUsers = new HashMap<>();
		newUsers = new HashMap<>();
		userActions = new HashMap<>();

		setTitle("Klasse hinzufügen");
		setIconImage(IconProvider.CLASS.getImage());
		setModalityType(ModalityType.APPLICATION_MODAL);
		setResizable(false);
		setBounds(100, 100, 507, 544);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());

		ClassValidator validator = new ClassValidator(this);
		{
			namePanel = new JPanel();
			namePanel.setLayout(new MigLayout("", "[][grow,fill]", "[]2[22px:n]"));
			namePanel.setBorder(new EmptyBorder(5, 5, 0, 5));
			getContentPane().add(namePanel, BorderLayout.NORTH);
		}
		{
			JLabel nameLabel = new JLabel("Name:");
			namePanel.add(nameLabel, "cell 0 0,alignx left,aligny center");
		}
		{
			LimitedLengthDocument document = new LimitedLengthDocument(32);
			document.addUndoableEditListener(validator);
			nameField = new JTextField(document, "", 32);
			nameField.addFocusListener(validator);
			namePanel.add(nameField, "cell 1 0,alignx center,aligny center");
		}
		{
			JLabel resultLabel = ValidationResultViewFactory.createReportIconAndTextLabel(validator.getResultModel());
			namePanel.add(resultLabel, "cell 1 1");
		}
		{
			JPanel userPanel = new JPanel();
			userPanel.setBorder(new TitledBorder(null, "Lehrer und Sch\u00FCler der Klasse",
					TitledBorder.LEADING, TitledBorder.TOP, null, null));
			userPanel.setLayout(new BorderLayout(0, 0));
			{
				JPanel buttonPanel = new JPanel();
				FlowLayout flowLayout = (FlowLayout) buttonPanel.getLayout();
				flowLayout.setHgap(2);
				flowLayout.setVgap(3);
				flowLayout.setAlignment(FlowLayout.LEFT);
				userPanel.add(buttonPanel, BorderLayout.NORTH);
				{
					JButton userAddButton = new JButton("Nutzer hinzufügen", IconProvider.USER_ADD);
					userAddButton.addActionListener(new ActionListener() {

						public void actionPerformed(ActionEvent e) {
							UserDialog userDialog = new UserDialog(host);
							userDialog.setStudentClass(editingClass, true);
							userDialog.filterUsernames(getUsernames());
							userDialog.setVisible(true);

							Action[] actions = userDialog.getCreatedActions();
							if (actions.length == 0) return;

							ActionUser action = (ActionUser) actions[0];
							User user = fromAction(action, null);

							newUsers.put(user.getLocalId(), user);
							userActions.put(user.getLocalId(), action);
							userTable.addUser(user, host);
						}
					});
					buttonPanel.add(userAddButton);
				}
			}
			{
				userTable = new UserTablePanel(RoleMode.SHOW_BOTH, false);
				userTable.getTable().addMouseListener(new MouseAdapter() {

					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
							User user = userTable.getSelectedUser();
							if (user == null) return;

							boolean isNew = !originalUsers.containsKey(user.getLocalId());

							UserDialog dialog = new UserDialog(host);
							dialog.setStudentClass(editingClass, true);
							dialog.setData(user);
							dialog.filterUsernames(getUsernames());
							dialog.setVisible(true);

							Action[] actions = dialog.getCreatedActions();
							if (actions.length == 0) return;
							ActionUser action = (ActionUser) actions[0];

							if (isNew) {
								if (action instanceof ActionUserUpdated) {
									ActionUserUpdated update = (ActionUserUpdated) action;
									User updatedUser = fromAction(update, user);

									ActionUserCreated create = new ActionUserCreated(updatedUser);
									newUsers.put(user.getLocalId(), updatedUser);
									userActions.put(user.getLocalId(), create);
								} else if (action instanceof ActionUserRemoved) {
									newUsers.remove(user.getLocalId());
									userActions.remove(user.getLocalId());
								}
							} else {
								User original = originalUsers.get(user.getLocalId());
								if (original == null) throw new IllegalStateException(); // can't happen

								if (action instanceof ActionUserUpdated) {
									User updatedUser = fromAction(action, original);
									ActionUserUpdated update = new ActionUserUpdated(updatedUser);

									newUsers.put(user.getLocalId(), updatedUser);
									userActions.put(user.getLocalId(), update);
								} else if (action instanceof ActionUserRemoved) {
									ActionUserRemoved removed = new ActionUserRemoved(original);

									newUsers.remove(user.getLocalId());
									originalUsers.remove(user.getLocalId());
									userActions.put(user.getLocalId(), removed);
								}
							}

							HashMap<Integer, User> allUsers = new HashMap<>(originalUsers);
							allUsers.putAll(newUsers);

							userTable.loadData(allUsers.values(), host);
							userTable.selectUser(user);
						}
					}
				});
				userPanel.add(userTable, BorderLayout.CENTER);
			}
			getContentPane().add(userPanel, BorderLayout.CENTER);
		}
		{
			JPanel buttonPanel = new JPanel();
			FlowLayout flowLayout = (FlowLayout) buttonPanel.getLayout();
			flowLayout.setHgap(2);
			flowLayout.setAlignment(FlowLayout.LEFT);
			{
				JButton userAddButton = new JButton("Neuer Nutzer erstellen");
				userAddButton.setMargin(new Insets(0, -3, 1, 0));
				userAddButton.setIconTextGap(10);
				userAddButton.setIcon(IconProvider.USER_ADD);
				buttonPanel.add(userAddButton);
			}
		}
		{
			JPanel buttonPane = new JPanel();
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
	}

	private static @NonNull User fromAction(ActionUser actionUser, @Nullable User original) {
		int id = (original != null) ? original.getLocalId() : actionUser.getUserId();
		MonetaryValue value = (original != null) ? original.getBalance() : MonetaryValue.ZERO;
		return new User(id, actionUser.getClassId(), actionUser.getRoleId(),
				actionUser.getFirstName(), actionUser.getLastName(), actionUser.getUsername(), value);
	}

	private Collection<String> getUsernames() {
		List<String> addresses = new ArrayList<>();
		for (User user : newUsers.values()) {
			addresses.add(user.getUsername());
		}

		return addresses;
	}

	public void setData(@NonNull StudentClass studentClass) {
		editingClass = studentClass;
		nameField.setText(studentClass.getName());
		isEdit = true;
		originalName = studentClass.getName();

		originalUsers.clear();
		newUsers.clear();
		deleteButton.setVisible(PermissionsHelper.canUpdateClass(host, studentClass, UpdateType.REMOVAL));

		originalUsers.putAll(host.getUsersByClass(studentClass.getLocalId()));
		userTable.loadData(originalUsers.values(), host);

		setTitle("Klasse bearbeiten");
	}

	public void addNewUsers(Collection<User> users) {
		for (User user : users) {
			if (user == null) continue;

			newUsers.put(user.getLocalId(), user);
			userActions.put(user.getLocalId(), new ActionUserCreated(user));
			userTable.addUser(user, host);
		}
	}

	private Action[] createActions() {
		List<Action> actions = new ArrayList<>();
		int id = editingClass.getLocalId();
		String name = checkValidity(nameField.getText());
		StudentClass newClass = new StudentClass(id, name, editingClass.getRoundingValue(), editingClass.getRoundedBalance());

		if (isEdit) {
			if (!newClass.getName().equals(originalName)) {
				actions.add(new ActionClassUpdated(newClass));
			}
		} else {
			actions.add(new ActionClassCreated(newClass));
		}

		actions.addAll(userActions.values());

		return actions.toArray(new Action[0]);
	}

	public @NonNull Action[] getCreatedActions() {
		return createdActions;
	}

	@SuppressWarnings("null")
	@Override
	public void actionPerformed(ActionEvent e) {
		if ("OK".equals(e.getActionCommand())) {
			createdActions = createActions();
		} else if ("Delete".equals(e.getActionCommand())) {
			if (!editingClass.getBalance().equals(MonetaryValue.ZERO)) {
				if (!Dialogs.confirmClassDeletion(editingClass)) return;
			}

			createdActions = new Action[] {new ActionClassRemoved(editingClass)};
		}
		dispose();
	}

	private static @NonNull <T> T checkValidity(T value) {
		if (value == null) throw new IllegalStateException();
		if (value instanceof String && ((String) value).isEmpty()) throw new IllegalStateException();
		return value;
	}

	private static class ClassValidator extends FocusAdapter implements UndoableEditListener, Validator<ClassDialog> {

		private final ValidationResultModel validationResultModel;
		private final ClassDialog parent;

		public ClassValidator(ClassDialog classDialog) {
			validationResultModel = new DefaultValidationResultModel();
			parent = classDialog;
		}

		public ValidationResultModel getResultModel() {
			return validationResultModel;
		}

		@Override
		public void focusLost(FocusEvent e) {
			validate(parent);
		}

		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			validate(parent);
		}

		@Override
		public ValidationResult validate(ClassDialog validationTarget) {
			ValidationResult result = new ValidationResult();

			String name = validationTarget.nameField.getText();
			if (name == null || name.isEmpty()) {
				result.addError("Geben Sie einen Namen für die Klasse ein.");
			} else {
				validationTarget.editingClass.setName(name);

				if (name.contains("Klasse")) {
					result.addWarning("Der Name sollte das Wort 'Klasse' nicht beinhalten.");
				} else if (!name.contains("20")) {
					result.addWarning("Der Name sollte eine Jahreszahl beinhalten, um eindeutig zu sein.");
				}
			}

			validationTarget.okButton.setEnabled(!result.hasErrors());

			validationResultModel.setResult(result);
			return result;
		}
	}
}
