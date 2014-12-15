package ch.kanti_wohlen.klassenkasse.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersAdded;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersRemoved;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentCreated;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentRemoved;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentUpdated;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.components.UserSelector;
import ch.kanti_wohlen.klassenkasse.ui.util.CurrencyFormat;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.ui.util.LimitedLengthDocument;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import ch.kanti_wohlen.klassenkasse.util.PaymentHelper;

import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;
import com.jgoodies.validation.Validator;
import com.jgoodies.validation.extras.IconFeedbackPanel;
import com.jgoodies.validation.util.DefaultValidationResultModel;
import com.jgoodies.validation.view.ValidationComponentUtils;
import com.jgoodies.validation.view.ValidationResultViewFactory;
import com.jidesoft.swing.CheckBoxTree;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JMonthChooser;
import com.toedter.calendar.JTextFieldDateEditor;

public class PaymentDialog extends JDialog implements ActionListener {

	private enum AmountInputMode {
		NONE,
		SINGLE,
		COMBINED
	}

	private static final Color SELECTED_BACKGROUND = new Color(235, 245, 255);

	private final @NonNull Host host;
	private final ValueUpdater valueUpdater;
	private final PaymentValidator validator;

	private final JPanel contentPanel;
	private final IconFeedbackPanel iconPanel;
	private final JDateChooser dateChooser;
	private final JTextField descriptionTextField;
	private final JComboBox<String> paymentTypeComboBox;
	private final JLabel valueLabel;
	private final JFormattedTextField singleValueTextField;
	private final JFormattedTextField combinedValueTextField;
	private final UserSelector userSelector;
	private final JButton okButton;
	private final JButton deleteButton;

	private Payment editingPayment;
	private @NonNull Map<Integer, User> originalPaymentUsers;
	private AmountInputMode inputMode;
	private @NonNull Action[] createdActions;
	private boolean roundingRequired;

	/**
	 * Create the dialog.
	 */
	public PaymentDialog(final @NonNull Host host) {
		this.host = host;
		inputMode = AmountInputMode.NONE;
		createdActions = new Action[0];
		valueUpdater = new ValueUpdater();
		validator = new PaymentValidator(this);
		originalPaymentUsers = new HashMap<>();

		setTitle("Zahlung hinzufügen");
		setIconImage(IconProvider.PAYMENT.getImage());
		setResizable(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setBounds(100, 100, 594, 486);

		getContentPane().setLayout(new BorderLayout());
		contentPanel = new JPanel();
		contentPanel.setBorder(null);
		contentPanel.setLayout(new MigLayout("", "[grow,fill]", "[][][grow,fill]10px[24px:n]"));

		iconPanel = new IconFeedbackPanel(validator.getValidationResultModel(), contentPanel);
		getContentPane().add(iconPanel, BorderLayout.CENTER);
		{
			JPanel descriptionPanel = new JPanel();
			descriptionPanel.setLayout(new MigLayout("", "[]15[grow,fill][grow 30]", "[grow][][]"));
			{
				JLabel dateLabel = new JLabel("Datum");
				descriptionPanel.add(dateLabel, "cell 0 0");
			}
			{
				JTextFieldDateEditor editor = new JTextFieldDateEditor();
				dateChooser = new JDateChooser(new Date(), null, editor);
				editor.addFocusListener(validator);

				// Fix popup visual glitches
				JMonthChooser monthChooser = dateChooser.getJCalendar().getMonthChooser();
				monthChooser.getComboBox().setPreferredSize(new Dimension(120, 20));
				((JComponent) monthChooser.getParent()).setOpaque(false);
				monthChooser.setOpaque(false);

				descriptionPanel.add(dateChooser, "cell 1 0,grow");
			}
			{
				JLabel descriptionLabel = new JLabel("Beschreibung");
				descriptionPanel.add(descriptionLabel, "cell 0 1");
			}
			{
				LimitedLengthDocument document = new LimitedLengthDocument(128);
				descriptionTextField = new JTextField(document, "", 128);
				descriptionTextField.addFocusListener(validator);
				document.addUndoableEditListener(new UndoableEditListener() {

					@Override
					public void undoableEditHappened(UndoableEditEvent e) {
						validator.validateComponent(descriptionTextField);
					}
				});
				descriptionPanel.add(descriptionTextField, "cell 1 1,growx");
			}
			{
				JLabel typeLabel = new JLabel("Typ");
				descriptionPanel.add(typeLabel, "cell 0 2");
			}
			{
				paymentTypeComboBox = new JComboBox<String>();
				paymentTypeComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"Gutschrift", "Belastung"}));
				paymentTypeComboBox.setSelectedIndex(1);
				descriptionPanel.add(paymentTypeComboBox, "cell 1 2,growx");
			}
			contentPanel.add(descriptionPanel, "cell 0 0,growx,aligny top");
		}
		{
			JPanel valuePanel = new JPanel();
			valuePanel.setLayout(new MigLayout("gapx 15", "[200,grow,fill][][fill][][200,grow,fill]", "[][]"));
			{
				JLabel singleValueLabel = new JLabel(" Einzelbetrag");
				valuePanel.add(singleValueLabel, "cell 0 0");
			}
			{
				JLabel amountLabel = new JLabel("Anzahl");
				valuePanel.add(amountLabel, "cell 2 0");
			}
			{
				JLabel combinedValueLabel = new JLabel(" Gesamtbetrag");
				valuePanel.add(combinedValueLabel, "cell 4 0");
			}
			{
				singleValueTextField = new JFormattedTextField(CurrencyFormat.NON_NEGATIVE);
				singleValueTextField.setHorizontalAlignment(SwingConstants.TRAILING);
				singleValueTextField.setValue(MonetaryValue.ZERO);
				singleValueTextField.addFocusListener(new FocusAdapter() {

					@Override
					public void focusGained(FocusEvent e) {
						setInputMode(AmountInputMode.SINGLE);
					}
				});
				singleValueTextField.addFocusListener(validator);
				singleValueTextField.addFocusListener(valueUpdater);
				singleValueTextField.addActionListener(valueUpdater);

				valuePanel.add(singleValueTextField, "flowy,cell 0 1,growx");
			}
			{
				JLabel xLabel = new JLabel("x");
				valuePanel.add(xLabel, "cell 1 1");
			}
			{
				valueLabel = new JLabel("0");
				valueLabel.setHorizontalAlignment(SwingConstants.TRAILING);
				valuePanel.add(valueLabel, "cell 2 1,alignx right");
			}
			{
				JLabel equalsLabel = new JLabel("=");
				valuePanel.add(equalsLabel, "cell 3 1,alignx trailing");
			}
			{
				combinedValueTextField = new JFormattedTextField(CurrencyFormat.NON_NEGATIVE);
				combinedValueTextField.setHorizontalAlignment(SwingConstants.TRAILING);
				combinedValueTextField.setValue(MonetaryValue.ZERO);
				combinedValueTextField.addFocusListener(new FocusAdapter() {

					@Override
					public void focusGained(FocusEvent e) {
						setInputMode(AmountInputMode.COMBINED);
					}
				});
				combinedValueTextField.addFocusListener(validator);
				combinedValueTextField.addFocusListener(valueUpdater);
				combinedValueTextField.addActionListener(valueUpdater);

				valuePanel.add(combinedValueTextField, "cell 4 1,growx");
			}
			contentPanel.add(valuePanel, "cell 0 1,grow");
		}
		{
			userSelector = new UserSelector();
			userSelector.loadData(host);
			CheckBoxTree tree = userSelector.getCheckBoxTree();
			tree.addFocusListener(validator);
			tree.getCheckBoxTreeSelectionModel().addTreeSelectionListener(valueUpdater);
			contentPanel.add(userSelector, "cell 0 2,grow");
		}
		{
			ValidationComponentUtils.setMessageKey(dateChooser.getDateEditor().getUiComponent(), "date");
			ValidationComponentUtils.setMessageKey(descriptionTextField, "description");
			ValidationComponentUtils.setMessageKey(singleValueTextField, "singleValue");
			ValidationComponentUtils.setMessageKey(combinedValueTextField, "combinedValue");
			ValidationComponentUtils.setMessageKey(userSelector.getCheckBoxTree(), "users");

			JLabel errorLabel = ValidationResultViewFactory.createReportIconAndTextLabel(validator.getValidationResultModel());
			contentPanel.add(errorLabel, "cell 0 3,grow");
		}
		{
			JPanel buttonPane = new JPanel();
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			buttonPane.setLayout(new MigLayout("", "[]push[][]", "[]"));
			{
				deleteButton = new JButton("Zahlung löschen");
				deleteButton.setActionCommand("Delete");
				deleteButton.setVisible(false);
				deleteButton.setIcon(IconProvider.DELETE);
				deleteButton.addActionListener(this);
				buttonPane.add(deleteButton, "cell 0 0,alignx left,aligny top");
			}
			{
				okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.setEnabled(false);
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

	private void setInputMode(@NonNull AmountInputMode mode) {
		inputMode = mode;

		switch (mode) {
		case SINGLE:
			singleValueTextField.setBackground(SELECTED_BACKGROUND);
			combinedValueTextField.setBackground(Color.WHITE);
			break;
		case COMBINED:
			singleValueTextField.setBackground(Color.WHITE);
			combinedValueTextField.setBackground(SELECTED_BACKGROUND);
			break;
		default:
			singleValueTextField.setBackground(Color.WHITE);
			combinedValueTextField.setBackground(Color.WHITE);
			break;
		}
	}

	public void setData(@NonNull Payment payment) {
		editingPayment = payment;

		dateChooser.setDate(payment.getDate());
		descriptionTextField.setText(payment.getDescription());

		originalPaymentUsers = host.getUsersWithPayment(payment.getLocalId());
		int users = originalPaymentUsers.size();
		valueLabel.setText(String.valueOf(users));

		for (User user : originalPaymentUsers.values()) {
			userSelector.ensureUserAdded(host, user);
			userSelector.checkByUserObject(user, true, true);
		}

		MonetaryValue value = payment.getValue();
		singleValueTextField.setValue(value.abs());

		MonetaryValue combined = value.multiply(users).add(payment.getRoundingValue());
		combinedValueTextField.setValue(combined.abs());

		if (payment.getRoundingValue().equals(MonetaryValue.ZERO)) {
			if (value.signum() < 0) {
				paymentTypeComboBox.setSelectedIndex(1);
			} else {
				paymentTypeComboBox.setSelectedIndex(0);
			}

			validator.editedComponents.add(singleValueTextField);
			setInputMode(AmountInputMode.SINGLE);
		} else {
			if (combined.signum() < 0) {
				paymentTypeComboBox.setSelectedIndex(1);
			} else {
				paymentTypeComboBox.setSelectedIndex(0);
			}

			validator.editedComponents.add(combinedValueTextField);
			setInputMode(AmountInputMode.COMBINED);
		}

		validator.editedComponents.add(userSelector);
		validator.validate(this);
		deleteButton.setVisible(true);

		setTitle("Zahlung bearbeiten");
	}

	public void selectAll(@NonNull Host host) {
		userSelector.checkByUserObject(host, true, false);
	}

	public void selectStudentClass(@NonNull StudentClass studentClass) {
		userSelector.checkByUserObject(studentClass, true, true);
	}

	public void selectUser(@NonNull User user) {
		userSelector.checkByUserObject(user, true, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ("OK".equals(e.getActionCommand())) {
			createNewOrEditedActions();
		} else if ("Delete".equals(e.getActionCommand())) {
			createRemovedActions();
		}
		dispose();
	}

	private void createNewOrEditedActions() {
		Payment newPayment = createPayment();
		Collection<User> users = userSelector.getSelectedUsers();

		if (editingPayment == null) {
			ActionPaymentCreated created = new ActionPaymentCreated(newPayment);
			ActionPaymentUsersAdded usersAdded = new ActionPaymentUsersAdded(newPayment, users);

			createdActions = new Action[] {created, usersAdded};
		} else {
			Collection<Action> actions = new ArrayList<>();
			if (!newPayment.getDate().equals(editingPayment.getDate())
					|| !newPayment.getDescription().equals(editingPayment.getDescription())
					|| !newPayment.getValue().equals(editingPayment.getValue())
					|| !newPayment.getRoundingValue().equals(editingPayment.getRoundingValue())) {
				actions.add(new ActionPaymentUpdated(newPayment));
			}

			Collection<User> existingUsers = host.getUsersWithPayment(editingPayment.getLocalId()).values();
			Collection<User> addedUsers = new ArrayList<>(users);
			addedUsers.removeAll(existingUsers);
			if (!addedUsers.isEmpty()) {
				actions.add(new ActionPaymentUsersAdded(newPayment, addedUsers));
			}

			Collection<User> removedUsers = new ArrayList<User>(existingUsers);
			removedUsers.removeAll(users);
			if (!removedUsers.isEmpty()) {
				actions.add(new ActionPaymentUsersRemoved(newPayment, removedUsers));
			}

			createdActions = checkValidity(actions.toArray(new Action[0]));
		}
	}

	@SuppressWarnings("null")
	private void createRemovedActions() {
		Map<Integer, User> paymentUsers = host.getUsersWithPayment(editingPayment.getLocalId());
		ActionPaymentRemoved payment = new ActionPaymentRemoved(editingPayment);
		ActionPaymentUsersRemoved users = new ActionPaymentUsersRemoved(editingPayment, paymentUsers.values());

		createdActions = new Action[] {users, payment};
	}

	private @NonNull Payment createPayment() {
		Date date = checkValidity(dateChooser.getDate());
		String description = checkValidity(descriptionTextField.getText());

		Collection<User> oldUsers = originalPaymentUsers.values();
		Collection<User> newUsers = userSelector.getSelectedUsers();
		MonetaryValue previousRounding = MonetaryValue.ZERO;
		if (editingPayment != null) previousRounding = editingPayment.getRoundingValue();

		MonetaryValue unroundedCombined;
		if (inputMode == AmountInputMode.SINGLE) {
			MonetaryValue unroundedSingle = checkValidity((MonetaryValue) singleValueTextField.getValue());
			unroundedCombined = unroundedSingle.multiply(newUsers.size());
		} else if (inputMode == AmountInputMode.COMBINED) {
			unroundedCombined = checkValidity((MonetaryValue) combinedValueTextField.getValue());
		} else {
			throw new IllegalStateException("Input mode");
		}

		if (paymentTypeComboBox.getSelectedIndex() == 1) {
			unroundedCombined = unroundedCombined.negate();
		}

		@SuppressWarnings("null")
		MonetaryValue combined = PaymentHelper.getBestRoundingValue(host, unroundedCombined, previousRounding, oldUsers, newUsers);
		MonetaryValue singleValue = new MonetaryValue(combined.getCentValue() / newUsers.size());
		MonetaryValue rounding = unroundedCombined.subtract(combined);

		if (editingPayment == null) {
			return new Payment(host, date, description, singleValue, rounding);
		} else {
			return new Payment(editingPayment.getLocalId(), date, description, singleValue, rounding);
		}
	}

	private @NonNull <T> T checkValidity(T o) {
		if (o == null) throw new IllegalStateException();
		if (o instanceof String && ((String) o).isEmpty()) throw new IllegalStateException();
		return o;
	}

	public @NonNull Action[] getCreatedActions() {
		return createdActions;
	}

	private class ValueUpdater extends FocusAdapter implements TreeSelectionListener, ActionListener {

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			update();
		}

		@Override
		public void focusLost(FocusEvent e) {
			commitIfPossible(e.getSource());
			update();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			commitIfPossible(source);
			update();

			if (source instanceof JFormattedTextField) {
				JFormattedTextField textField = (JFormattedTextField) source;
				Object value = textField.getValue();

				if (value != null) {
					textField.setValue(value);
				}
				validator.validateComponent(textField);
			}
		}

		private void commitIfPossible(Object source) {
			if (source instanceof JFormattedTextField) {
				try {
					((JFormattedTextField) source).commitEdit();
				} catch (ParseException e) {}
			}
		}

		private void update() {
			int count = userSelector.getSelectedUsers().size();
			valueLabel.setText(String.valueOf(count));

			if (count == 0) {
				switch (inputMode) {
				case SINGLE:
					combinedValueTextField.setValue(MonetaryValue.ZERO);
					break;
				case COMBINED:
					singleValueTextField.setValue(MonetaryValue.ZERO);
					break;
				default:
					break;
				}

				validator.validateComponent(userSelector.getCheckBoxTree());
				return;
			}

			roundingRequired = false;
			switch (inputMode) {
			case SINGLE:
				MonetaryValue single = (MonetaryValue) singleValueTextField.getValue();
				combinedValueTextField.setValue(single.multiply(count));
				if (single.getCentValue() % 5 != 0) {
					roundingRequired = true;
				}
				break;
			case COMBINED:
				MonetaryValue combined = (MonetaryValue) combinedValueTextField.getValue();
				long singleValue = combined.getCentValue() / count;
				singleValueTextField.setValue(new MonetaryValue(singleValue));
				if (combined.getCentValue() % (5 * count) != 0) {
					roundingRequired = true;
				}
				break;
			default:
				break;
			}

			validator.validateComponent(userSelector.getCheckBoxTree());
		}
	}

	private static class PaymentValidator extends FocusAdapter implements Validator<PaymentDialog> {

		private final ValidationResultModel validationResultModel;
		private final PaymentDialog parent;
		private final HashSet<Component> editedComponents;

		public PaymentValidator(PaymentDialog paymentDialog) {
			validationResultModel = new DefaultValidationResultModel();
			parent = paymentDialog;
			editedComponents = new HashSet<>();
		}

		public ValidationResultModel getValidationResultModel() {
			return validationResultModel;
		}

		@Override
		public void focusLost(FocusEvent e) {
			editedComponents.add(e.getComponent());
			validate(parent);
		}

		public void validateComponent(Component component) {
			editedComponents.add(component);
			validate(parent);
		}

		@Override
		public ValidationResult validate(PaymentDialog validationTarget) {
			ValidationResult result = new ValidationResult();
			boolean allChecked = true;

			Date date = validationTarget.dateChooser.getDate();
			if (date == null) {
				result.addError("Wählen Sie ein korrektes Datum.", "date");
			} else if (validationTarget.editingPayment == null && !sameMonth(date, new Date())) {
				result.addWarning("Das Datum der Zahlung ist um mehr als ein Monat verschieden.", "date");
			}

			if (editedComponents.contains(validationTarget.descriptionTextField)) {
				String description = validationTarget.descriptionTextField.getText();
				if (description == null || description.isEmpty()) {
					result.addError("Geben Sie eine Beschreibung für die Zahlung ein.", "description");
				}
			} else {
				allChecked = false;
			}

			if (editedComponents.contains(validationTarget.singleValueTextField)
					|| editedComponents.contains(validationTarget.combinedValueTextField)) {
				switch (validationTarget.inputMode) {
				case NONE:
					result.addError("Geben Sie den Zahlungsbetrag ein.", "combinedValue");
					break;
				case COMBINED:
					MonetaryValue combined = (MonetaryValue) validationTarget.combinedValueTextField.getValue();
					if (combined == null || combined.signum() == 0) {
						result.addError("Der Betrag der Zahlung kann nicht 0 sein.", "combinedValue");
					} else if (combined.signum() == -1) {
						result.addError("Der Betrag kann nicht negativ sein.", "combinedValue");
					}
					break;
				case SINGLE:
					MonetaryValue single = (MonetaryValue) validationTarget.singleValueTextField.getValue();
					if (single == null || single.signum() == 0) {
						result.addError("Der Betrag der Zahlung kann nicht 0 sein.", "singleValue");
					} else if (single.signum() == -1) {
						result.addError("Der Betrag kann nicht negativ sein.", "singleValue");
					}
					break;
				default:
					throw new IllegalStateException("Unknown amount input mode.");
				}

				if (validationTarget.roundingRequired) {
					result.addInfo("Der Betrag wird automatisch gerundet.");
				}
			} else {
				allChecked = false;
			}

			if (editedComponents.contains(validationTarget.userSelector.getCheckBoxTree())) {
				Collection<User> selectedUsers = validationTarget.userSelector.getSelectedUsers();
				if (selectedUsers.isEmpty()) {
					result.addError("Wählen Sie mindestens einen Nutzer.", "users");
				}
			} else {
				allChecked = false;
			}

			validationTarget.okButton.setEnabled(allChecked && !result.hasErrors());
			validationResultModel.setResult(result);
			return result;
		}

		private static boolean sameMonth(Date date1, Date date2) {
			long diff = Math.abs(date1.getTime() - date2.getTime());
			return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) < 30;
		}
	}
}
