package ch.kanti_wohlen.klassenkasse.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Field;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.ActionSearchQuery;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActions;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsRedone;
import ch.kanti_wohlen.klassenkasse.action.actions.ActionActionsUndone;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClass;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassCreated;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassRemoved;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassUpdated;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsers;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersAdded;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersRemoved;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPayment;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentCreated;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentRemoved;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentUpdated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUser;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserCreated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserRemoved;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserUpdated;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.components.ActionTablePanel;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.StudentClassComparator;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.UserComparator;

import com.camick.swing.WrapLayout;
import com.toedter.calendar.JTextFieldDateEditor;

// TODO: Causes de-sync
public class HistoryPanel extends JPanel {

	private static final Map<JLabel, Class<? extends Action>> ACTION_TYPE_NAMES = new LinkedHashMap<>();

	private static void put(String text, Icon icon, boolean bold, Class<? extends Action> value) {
		JLabel label = new JLabel(text);
		if (icon != null) {
			label.setIcon(icon);
		}
		if (bold) {
			label.setFont(label.getFont().deriveFont(Font.BOLD));
		}

		ACTION_TYPE_NAMES.put(label, value);
	}

	static {
		put("Alle Aktionstypen", null, true, null);
		put("Klassen", IconProvider.CLASS, true, ActionClass.class);
		put("Klasse erstellt", IconProvider.CLASS_ADD, false, ActionClassCreated.class);
		put("Klasse verändert", IconProvider.CLASS_EDIT, false, ActionClassUpdated.class);
		put("Klasse gelöscht", IconProvider.CLASS_REMOVE, false, ActionClassRemoved.class);

		put("Benutzer", IconProvider.USER, true, ActionUser.class);
		put("Benutzer erstellt", IconProvider.USER_ADD, false, ActionUserCreated.class);
		put("Benutzer verändert", IconProvider.USER_EDIT, false, ActionUserUpdated.class);
		put("Benutzer gelöscht", IconProvider.USER_REMOVE, false, ActionUserRemoved.class);

		put("Zahlungen", IconProvider.PAYMENT, true, ActionPayment.class);
		put("Zahlung erstellt", IconProvider.PAYMENT_ADD, false, ActionPaymentCreated.class);
		put("Zahlung verändert", IconProvider.PAYMENT_EDIT, false, ActionPaymentUpdated.class);
		put("Zahlung gelöscht", IconProvider.PAYMENT_REMOVE, false, ActionPaymentRemoved.class);

		put("Nutzer in Zahlungen", IconProvider.USER, true, ActionPaymentUsers.class);
		put("Nutzer zu Zahlung hinzugefügt", IconProvider.USER_ADD, false, ActionPaymentUsersAdded.class);
		put("Nutzer von Zahlung entfernt", IconProvider.USER_REMOVE, false, ActionPaymentUsersRemoved.class);

		put("Aktionen", IconProvider.UNDO, true, ActionActions.class);
		put("Aktionen rückgängig gemacht", IconProvider.UNDO, false, ActionActionsUndone.class);
		put("Aktionen wiederholt", IconProvider.REDO, false, ActionActionsRedone.class);
	}

	private final ActionTablePanel actionTablePanel;
	private final JButton redoButton;
	private final JButton undoButton;
	private final JComboBox<User> creatorComboBox;
	private final JComboBox<JLabel> actionTypeComboBox;
	private final JComboBox<String> undoneComboBox;
	private final JTextFieldDateEditor beforeDateField;
	private final JTextFieldDateEditor afterDateField;
	private final JComboBox<StudentClass> classComboBox;
	private final JPanel classPanel;
	private final JPanel userPanel;
	private final JComboBox<User> userComboBox;
	private final JComboBox<Integer> limitComboBox;

	/**
	 * Create the panel.
	 */
	public HistoryPanel(final @NonNull MainWindow mainWindow) {
		ActionListener searchActionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				search(mainWindow.getHost());
			}
		};
		FocusListener searchFocusListener = new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				search(mainWindow.getHost());
			}
		};

		setLayout(new MigLayout("", "[grow]", "[]10[][grow][]"));

		JLabel titleLabel = new JLabel("Suchparameter:");
		titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
		add(titleLabel, "cell 0 0,gapx 4");

		JPanel searchPanel = new JPanel();
		searchPanel.setBorder(new EmptyBorder(0, -15, 0, 0));
		add(searchPanel, "cell 0 1,grow");
		searchPanel.setLayout(new WrapLayout(WrapLayout.LEFT, 15, 0));

		JPanel creatorPanel = new JPanel();
		searchPanel.add(creatorPanel);

		JLabel creatorLabel = new JLabel("Ersteller:");
		creatorPanel.add(creatorLabel);

		creatorComboBox = new JComboBox<>();
		creatorComboBox.setRenderer(new IconListCellRenderer());
		creatorComboBox.addActionListener(searchActionListener);
		creatorPanel.add(creatorComboBox);

		JPanel actionTypePanel = new JPanel();
		searchPanel.add(actionTypePanel);

		JLabel actionTypeLabel = new JLabel("Aktionstyp:");
		actionTypePanel.add(actionTypeLabel);

		actionTypeComboBox = new JComboBox<>();
		actionTypeComboBox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JLabel value = (JLabel) actionTypeComboBox.getSelectedItem();
				Class<? extends Action> actionType = ACTION_TYPE_NAMES.get(value);

				classPanel.setVisible(false);
				userPanel.setVisible(false);

				if (actionType == null) {
					search(mainWindow.getHost());
					return;
				} else if (ActionClass.class.isAssignableFrom(actionType)) {
					classComboBox.setSelectedIndex(0); // calls search()
					classPanel.setVisible(true);
				} else if (ActionUser.class.isAssignableFrom(actionType)
						|| ActionPaymentUsers.class.isAssignableFrom(actionType)) {
					userComboBox.setSelectedIndex(0); // calls search()
					userPanel.setVisible(true);
				} else {
					search(mainWindow.getHost());
				}
			}
		});
		actionTypePanel.add(actionTypeComboBox);
		actionTypeComboBox.setRenderer(new IconListCellRenderer());

		JPanel undonePanel = new JPanel();
		searchPanel.add(undonePanel);

		JLabel undoneLabel = new JLabel("Rückgängig:");
		undonePanel.add(undoneLabel);

		undoneComboBox = new JComboBox<>();
		undoneComboBox.setModel(new DefaultComboBoxModel<>(new String[] {"Beide", "Ja", "Nein"}));
		undoneComboBox.addActionListener(searchActionListener);
		undonePanel.add(undoneComboBox);

		JPanel beforePanel = new JPanel();
		searchPanel.add(beforePanel);
		beforePanel.setLayout(new MigLayout("insets 5 5", "[][80px,fill]", "[]"));

		JLabel beforeLabel = new JLabel("Vor:");
		beforePanel.add(beforeLabel, "cell 0 0,alignx left,aligny center");

		beforeDateField = new JTextFieldDateEditor();
		removeGreenColor(beforeDateField);
		Calendar tomorrow = Calendar.getInstance();
		tomorrow.add(Calendar.DATE, 1);
		beforeDateField.setValue(tomorrow.getTime());
		beforeDateField.addFocusListener(searchFocusListener);
		beforePanel.add(beforeDateField, "cell 1 0,alignx left,aligny top");

		JPanel afterPanel = new JPanel();
		searchPanel.add(afterPanel);
		afterPanel.setLayout(new MigLayout("", "[][80px,fill]", "[]"));

		JLabel afterLabel = new JLabel("Nach:");
		afterPanel.add(afterLabel, "cell 0 0,alignx left,aligny center");

		afterDateField = new JTextFieldDateEditor();
		removeGreenColor(afterDateField);
		afterDateField.setValue(new Date(0));
		afterDateField.addFocusListener(searchFocusListener);
		afterPanel.add(afterDateField, "cell 1 0,alignx left,aligny top");

		classPanel = new JPanel();
		classPanel.setVisible(false);
		searchPanel.add(classPanel);

		JLabel classLabel = new JLabel("Klasse:");
		classPanel.add(classLabel);

		classComboBox = new JComboBox<>();
		classComboBox.setRenderer(new IconListCellRenderer());
		classComboBox.addActionListener(searchActionListener);
		classPanel.add(classComboBox);

		userPanel = new JPanel();
		userPanel.setVisible(false);
		searchPanel.add(userPanel);

		JLabel userLabel = new JLabel("Benutzer:");
		userPanel.add(userLabel);

		userComboBox = new JComboBox<>();
		userComboBox.setRenderer(new IconListCellRenderer());
		userComboBox.addActionListener(searchActionListener);
		userPanel.add(userComboBox);

		actionTablePanel = new ActionTablePanel();
		actionTablePanel.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				BaseAction action = actionTablePanel.getSelectedAction();
				if (action == null) {
					undoButton.setEnabled(false);
					redoButton.setEnabled(false);
					return;
				}

				undoButton.setEnabled(action.isApplied());
				redoButton.setEnabled(action.isUndone());
			}
		});
		add(actionTablePanel, "cell 0 2,grow");

		JPanel actionPanel = new JPanel();
		add(actionPanel, "cell 0 3,grow");
		actionPanel.setLayout(new MigLayout("", "[][]push[][]", "[]"));

		limitComboBox = new JComboBox<>();
		limitComboBox.setModel(new DefaultComboBoxModel<>(new Integer[] {10, 20, 50, 100}));
		limitComboBox.setSelectedIndex(1);
		limitComboBox.addActionListener(searchActionListener);
		actionPanel.add(limitComboBox, "cell 0 0,growx");

		JLabel resultsLabel = new JLabel("Ergebnisse");
		actionPanel.add(resultsLabel, "cell 1 0");

		undoButton = new JButton("Rückgängig");
		undoButton.setEnabled(false);
		undoButton.setHorizontalTextPosition(SwingConstants.LEADING);
		undoButton.setIcon(IconProvider.UNDO);
		undoButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				BaseAction action = actionTablePanel.getSelectedAction();
				if (action == null) return;

				Host host = mainWindow.getHost();
				host.setActionsUndone(true, action);
				search(host);
			}
		});
		actionPanel.add(undoButton, "cell 2 0,alignx left,aligny top");

		redoButton = new JButton("Wiederholen");
		redoButton.setEnabled(false);
		redoButton.setIcon(IconProvider.REDO);
		redoButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				BaseAction action = actionTablePanel.getSelectedAction();
				if (action == null) return;

				Host host = mainWindow.getHost();
				host.setActionsUndone(false, action);
				search(host);
			}
		});
		actionPanel.add(redoButton, "cell 3 0,alignx left,aligny top");

		loadData(mainWindow);
	}

	private static void removeGreenColor(JTextFieldDateEditor dateEditor) {
		try {
			Field darkGreen = JTextFieldDateEditor.class.getDeclaredField("darkGreen");
			darkGreen.setAccessible(true);
			darkGreen.set(dateEditor, Color.BLACK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadData(final @NonNull MainWindow mainWindow) {
		Host host = mainWindow.getHost();
		Map<Integer, User> allUsers = host.getUsers();
		actionTablePanel.setUsers(allUsers);

		Vector<User> userList = new Vector<>(allUsers.values());
		userList.add(0, null);
		Collections.sort(userList, new UserComparator());

		Map<Integer, StudentClass> allClasses = host.getClasses();
		Vector<StudentClass> classList = new Vector<>(allClasses.values());
		classList.add(0, null);
		Collections.sort(classList, new StudentClassComparator());

		creatorComboBox.setModel(new DefaultComboBoxModel<>(userList));
		classComboBox.setModel(new DefaultComboBoxModel<>(classList));
		userComboBox.setModel(new DefaultComboBoxModel<>(userList));
		actionTypeComboBox.setModel(new DefaultComboBoxModel<>(new Vector<>(ACTION_TYPE_NAMES.keySet())));

		IconListTextFilterator filterator = new IconListTextFilterator();
		EventList<User> userEventList = GlazedLists.eventList(userList);
		EventList<StudentClass> classEventList = GlazedLists.eventList(classList);
		AutoCompleteSupport.install(creatorComboBox, userEventList, filterator, new UserFormat());
		AutoCompleteSupport.install(classComboBox, classEventList, filterator, new StudentClassFormat());
		AutoCompleteSupport.install(userComboBox, userEventList, filterator, new UserFormat());

		search(host);
	}

	private void search(Host host) {
		ActionSearchQuery query = new ActionSearchQuery();

		JLabel actionLabel = (JLabel) actionTypeComboBox.getSelectedItem();
		Class<? extends Action> actionType = ACTION_TYPE_NAMES.get(actionLabel);
		query.setActionType(actionType);

		query.setCreator((User) creatorComboBox.getSelectedItem());
		query.setBefore(beforeDateField.getDate());
		query.setAfter(afterDateField.getDate());

		switch (undoneComboBox.getSelectedIndex()) {
		case 0: // Both
			query.setApplied(null);
			break;
		case 1:
			query.setApplied(false);
			break;
		case 2:
			query.setApplied(true);
			break;
		default:
			throw new IllegalStateException();
		}

		if (classPanel.isVisible()) {
			query.setClass((StudentClass) classComboBox.getSelectedItem());
		}

		if (userPanel.isVisible()) {
			query.setUser((User) userComboBox.getSelectedItem());
		}

		int limit = (int) limitComboBox.getSelectedItem();
		query.setLimit((short) limit);

		Map<Long, BaseAction> actions = host.searchActions(query);
		actionTablePanel.loadData(host, actions.values());
	}

	private static class IconListCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (c == null || !(c instanceof JLabel)) return c;
			JLabel ret = (JLabel) c;

			if (value instanceof JLabel) {
				JLabel label = (JLabel) value;
				ret.setText(label.getText());
				ret.setIcon(label.getIcon());
				ret.setFont(label.getFont());
			} else if (value instanceof StudentClass) {
				StudentClass studentClass = (StudentClass) value;
				ret.setText(studentClass.getName());
			} else if (value instanceof User) {
				User user = (User) value;
				ret.setText(user.getFullName());
				ret.setIcon(IconProvider.forUser(user));
			} else if (value == null) {
				ret.setText("Alle");
				ret.setFont(ret.getFont().deriveFont(Font.BOLD));
			}

			return ret;
		}
	}

	private static class IconListTextFilterator implements TextFilterator<Object> {

		@Override
		public void getFilterStrings(List<String> baseList, Object element) {
			if (baseList == null) return;

			if (element instanceof JLabel) {
				baseList.add(((JLabel) element).getText());
			} else if (element instanceof StudentClass) {
				baseList.add(((StudentClass) element).getName());
			} else if (element instanceof User) {
				baseList.add(((User) element).getFullName());
			} else if (element == null) {
				baseList.add("Alle");
			}
		}
	}

	private static class UserFormat extends Format {

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			if (obj == null) return new StringBuffer("Alle");
			User user = (User) obj;
			return new StringBuffer(user.getFullName());
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			throw new UnsupportedOperationException();
		}
	}

	private static class StudentClassFormat extends Format {

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			if (obj == null) return new StringBuffer("Alle");
			StudentClass studentClass = (StudentClass) obj;
			return new StringBuffer(studentClass.getName());
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			throw new UnsupportedOperationException();
		}
	}
}
