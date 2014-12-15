package ch.kanti_wohlen.klassenkasse.ui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.RowFilter;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.util.ConditionalFormatting;
import ch.kanti_wohlen.klassenkasse.ui.util.FontProvider;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import net.miginfocom.swing.MigLayout;
import ch.kanti_wohlen.klassenkasse.ui.components.PaymentTablePanel;
import ch.kanti_wohlen.klassenkasse.ui.dialog.Dialogs;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import ch.kanti_wohlen.klassenkasse.util.PermissionsHelper;

import javax.swing.SwingConstants;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import ch.kanti_wohlen.klassenkasse.ui.components.SearchTextField;

public class UserPanel extends JPanel {

	private User user;
	private PaymentTablePanel paymentTable;
	private SearchTextField searchTextField;
	private RowFilter<TableModel, Integer> rowFilter;

	private JLabel nameLabel;
	private JLabel descriptionLabel;
	private JLabel balanceLabel;

	/**
	 * Create the panel.
	 * 
	 * @param mainWindow
	 *            the main window of the application
	 * @param user
	 *            the user to be displayed
	 */
	public UserPanel(final @NonNull MainWindow mainWindow, User user) {
		setLayout(new MigLayout("", "[grow]", "[][]15[][grow]"));

		nameLabel = new JLabel("");
		nameLabel.setFont(FontProvider.SUBTITLE_FONT);
		add(nameLabel, "cell 0 0,gapx 8");

		descriptionLabel = new JLabel();
		descriptionLabel.setFont(FontProvider.DEFAULT_FONT);
		add(descriptionLabel, "cell 0 1,gapx 8");

		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("ins 0 8 0 8", "[]15[]push[40%]", "[]"));
		add(panel, "cell 0 2,grow");

		JLabel balanceDescriptionLabel = new JLabel("Saldo:");
		balanceDescriptionLabel.setFont(FontProvider.DEFAULT_BOLD);
		panel.add(balanceDescriptionLabel, "cell 0 0");

		balanceLabel = new JLabel(MonetaryValue.ZERO.toString());
		balanceLabel.setFont(FontProvider.DEFAULT_BOLD);
		balanceLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		panel.add(balanceLabel, "cell 1 0");

		searchTextField = new SearchTextField();
		searchTextField.getDocument().addUndoableEditListener(new UndoableEditListener() {

			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) paymentTable.getTable().getRowSorter();
				sorter.allRowsChanged();
			}
		});
		panel.add(searchTextField, "cell 2 0,grow");

		rowFilter = new RowFilter<TableModel, Integer>() {

			@Override
			public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
				String text = searchTextField.getText();
				if (text == null || text.trim().isEmpty()) return true;

				Date date = (Date) entry.getValue(0);
				MonetaryValue value = (MonetaryValue) entry.getValue(2);

				String dateString = PaymentTablePanel.DATE_FORMAT.format(date);
				String description = entry.getStringValue(1);
				String valueString = value.getAmountString(false);

				String[] searchTerms = text.split("[\\s,;]");
				for (String searchTerm : searchTerms) {
					// If the row doesn't match the search term, don't include it
					if (!dateString.contains(searchTerm)
							&& !description.contains(searchTerm)
							&& !valueString.contains(searchTerm)) {
						return false;
					}
				}

				return true;
			}
		};

		paymentTable = new PaymentTablePanel();
		paymentTable.getTable().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					Payment payment = paymentTable.getSelectedPayment();
					if (payment == null) return;

					Dialogs.editPayment(mainWindow, payment);
					selectPayment(mainWindow, payment);
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					int row = paymentTable.getTable().rowAtPoint(e.getPoint());
					paymentTable.getTable().getSelectionModel().setSelectionInterval(row, row);

					Payment payment = paymentTable.getSelectedPayment();
					if (payment == null) return;

					JPopupMenu menu = popUpMenuForPayment(mainWindow, payment);
					menu.show(paymentTable.getTable(), e.getX(), e.getY());
				}
			}
		});
		add(paymentTable, "cell 0 3,grow");

		loadData(user, mainWindow.getHost());
	}

	public void loadData(User user, @NonNull Host host) {
		this.user = user;

		nameLabel.setText(user.getFullName());
		nameLabel.setIcon(IconProvider.forUser(user));

		StringBuilder description = new StringBuilder();
		description.append(user.getRole(host).getName());
		if (user.getStudentClassId() != 0) {
			description.append(" der Klasse ").append(user.getStudentClass(host).getName());
		}
		descriptionLabel.setText(description.toString());

		balanceLabel.setText(user.getBalance().toString());
		balanceLabel.setForeground(ConditionalFormatting.getUserColor(user.getBalance()));

		Collection<Payment> payments = host.getPaymentsByUser(user.getLocalId()).values();
		paymentTable.loadData(payments);
		TableRowSorter<? extends TableModel> sorter = (TableRowSorter<? extends TableModel>) paymentTable.getTable().getRowSorter();
		sorter.setSortsOnUpdates(true);
		sorter.setRowFilter(rowFilter);
	}

	public PaymentTablePanel getPaymentsTable() {
		return paymentTable;
	}

	private JPopupMenu popUpMenuForPayment(final @NonNull MainWindow mainWindow, final @NonNull Payment payment) {
		JPopupMenu menu = new JPopupMenu();

		Collection<User> paymentUsers = mainWindow.getHost().getUsersWithPayment(payment.getLocalId()).values();
		if (paymentUsers == null) paymentUsers = new ArrayList<>();

		JMenuItem edit = new JMenuItem("Zahlung bearbeiten", IconProvider.EDIT);
		edit.setFont(edit.getFont().deriveFont(Font.BOLD));
		edit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.editPayment(mainWindow, payment);
				selectPayment(mainWindow, payment);
			}
		});
		edit.setEnabled(PermissionsHelper.canUpdatePayment(mainWindow.getHost(), payment, paymentUsers, UpdateType.UPDATE));
		menu.add(edit);

		menu.addSeparator();

		JMenuItem remove = new JMenuItem("Nutzer von dieser Zahlung entfernen", IconProvider.REMOVE);
		remove.addActionListener(new ActionListener() {

			@SuppressWarnings("null")
			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.removeUserFromPayment(mainWindow, payment, user);
			}
		});
		remove.setEnabled(PermissionsHelper.canUpdatePayment(mainWindow.getHost(), payment, paymentUsers, UpdateType.UPDATE));
		menu.add(remove);

		JMenuItem delete = new JMenuItem("Zahlung löschen", IconProvider.DELETE);
		delete.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.deletePayment(mainWindow, payment);
			}
		});
		delete.setEnabled(PermissionsHelper.canUpdatePayment(mainWindow.getHost(), payment, paymentUsers, UpdateType.REMOVAL));
		menu.add(delete);

		return menu;
	}

	private static void selectPayment(MainWindow mainWindow, Payment payment) {
		UserPanel userPanel = (UserPanel) mainWindow.getContentPanel();
		userPanel.paymentTable.selectPayment(payment);
	}
}
