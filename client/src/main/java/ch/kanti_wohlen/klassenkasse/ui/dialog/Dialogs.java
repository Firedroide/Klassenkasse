package ch.kanti_wohlen.klassenkasse.ui.dialog;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassRemoved;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersRemoved;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentRemoved;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserRemoved;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketPrintingInformation;
import ch.kanti_wohlen.klassenkasse.printing.Printer;
import ch.kanti_wohlen.klassenkasse.ui.MainWindow;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

/**
 * Class to easily make dialogs appear and apply their actions via static methods
 * to remove boilerplate code from other UI classes.
 *
 * @author Roger Baumgartner
 */
@NonNullByDefault
public final class Dialogs {

	private Dialogs() {}

	// CLASSES
	public static void newClass(MainWindow mainWindow) {
		Host host = mainWindow.getHost();
		ClassDialog dialog = new ClassDialog(host);
		dialog.setVisible(true);

		applyActions(mainWindow, dialog.getCreatedActions());
	}

	public static void editClass(MainWindow mainWindow, StudentClass studentClass) {
		Host host = mainWindow.getHost();
		ClassDialog dialog = new ClassDialog(host);
		dialog.setData(studentClass);
		dialog.setVisible(true);

		Action[] actions = dialog.getCreatedActions();
		applyActions(mainWindow, actions);

		if (actions.length > 0 && actions[0] instanceof ActionClassRemoved) {
			mainWindow.displayHome();
		}
	}

	public static void deleteClass(MainWindow mainWindow, StudentClass studentClass) {
		ActionClassRemoved action = new ActionClassRemoved(studentClass);
		applyActions(mainWindow, action);

		mainWindow.displayHome();
	}

	public static boolean confirmClassDeletion(StudentClass studentClass) {
		StringBuilder message = new StringBuilder();
		message.append("Der Kontostand der Klasse ").append(studentClass.getName()).append(" beträgt nicht Fr. 0.-\n");
		message.append("Durch das Löschen dieser Klasse wird der Klassensaldo verändert!");

		int option = JOptionPane.OK_CANCEL_OPTION;
		int messageType = JOptionPane.WARNING_MESSAGE;
		Icon icon = IconProvider.CLASS_REMOVE;

		int result = JOptionPane.showConfirmDialog(null, message, "Klasse löschen", option, messageType, icon);
		return result == JOptionPane.OK_OPTION;
	}

	// USERS
	public static void newUser(MainWindow mainWindow) {
		Host host = mainWindow.getHost();
		UserDialog dialog = new UserDialog(host);
		dialog.setVisible(true);

		applyActions(mainWindow, dialog.getCreatedActions());
	}

	public static void newUser(MainWindow mainWindow, StudentClass studentClass) {
		Host host = mainWindow.getHost();
		UserDialog dialog = new UserDialog(host);
		dialog.setStudentClass(studentClass, false);
		dialog.setVisible(true);

		applyActions(mainWindow, dialog.getCreatedActions());
	}

	public static void editUser(MainWindow mainWindow, User user) {
		Host host = mainWindow.getHost();
		UserDialog dialog = new UserDialog(host);
		dialog.setData(user);
		dialog.setVisible(true);

		Action[] actions = dialog.getCreatedActions();
		applyActions(mainWindow, actions);

		if (actions.length > 0 && actions[0] instanceof ActionUserRemoved) {
			mainWindow.displayClass(user.getStudentClass(host));
		}
	}

	public static void deleteUser(MainWindow mainWindow, User user) {
		if (!user.getBalance().equals(MonetaryValue.ZERO)) {
			if (!confirmUserDeletion(user)) return;
		}

		Host host = mainWindow.getHost();
		ActionUserRemoved action = new ActionUserRemoved(user);
		applyActions(mainWindow, action);

		mainWindow.displayClass(user.getStudentClass(host));
	}

	public static boolean confirmUserDeletion(User user) {
		StringBuilder message = new StringBuilder();
		message.append("Der Kontostand des Nutzers ").append(user.getFullName()).append(" beträgt nicht Fr. 0.-\n");
		message.append("Durch das Löschen dieses Nutzers wird der Klassensaldo verändert!");

		int option = JOptionPane.OK_CANCEL_OPTION;
		int messageType = JOptionPane.WARNING_MESSAGE;
		Icon icon = IconProvider.USER_REMOVE;

		int result = JOptionPane.showConfirmDialog(null, message, "Nutzer löschen", option, messageType, icon);
		return result == JOptionPane.OK_OPTION;
	}

	// PAYMENTS
	public static void newPayment(MainWindow mainWindow) {
		Host host = mainWindow.getHost();
		PaymentDialog dialog = new PaymentDialog(host);
		dialog.selectAll(host);
		dialog.setVisible(true);

		applyActions(mainWindow, dialog.getCreatedActions());
	}

	public static void newPayment(MainWindow mainWindow, StudentClass studentClass) {
		Host host = mainWindow.getHost();
		PaymentDialog dialog = new PaymentDialog(host);
		dialog.selectStudentClass(studentClass);
		dialog.setVisible(true);

		applyActions(mainWindow, dialog.getCreatedActions());
	}

	public static void newPayment(MainWindow mainWindow, User user) {
		Host host = mainWindow.getHost();
		PaymentDialog dialog = new PaymentDialog(host);
		dialog.selectUser(user);
		dialog.setVisible(true);

		applyActions(mainWindow, dialog.getCreatedActions());
	}

	public static void editPayment(MainWindow mainWindow, Payment payment) {
		Host host = mainWindow.getHost();
		PaymentDialog dialog = new PaymentDialog(host);
		dialog.setData(payment);
		dialog.setVisible(true);

		applyActions(mainWindow, dialog.getCreatedActions());
	}

	public static void deletePayment(MainWindow mainWindow, Payment payment) {
		ActionPaymentRemoved removePayment = new ActionPaymentRemoved(payment);

		Collection<User> paymentUsers = mainWindow.getHost().getUsersWithPayment(payment.getLocalId()).values();
		@SuppressWarnings("null")
		ActionPaymentUsersRemoved removeUsers = new ActionPaymentUsersRemoved(payment, paymentUsers);

		applyActions(mainWindow, removeUsers, removePayment);
	}

	public static void removeUserFromPayment(MainWindow mainWindow, Payment payment, User user) {
		if (mainWindow.getHost().getUsersWithPayment(payment.getLocalId()).values().size() == 1) {
			deletePayment(mainWindow, payment);
		} else {
			ActionPaymentUsersRemoved action = new ActionPaymentUsersRemoved(payment, user);
			applyActions(mainWindow, action);
		}
	}

	// PRINTING
	public static void showPrintDialog(MainWindow mainWindow) {
		StudentClass studentClass;
		Host host = mainWindow.getHost();
		Object selectedObject = mainWindow.getSelectedObject();

		if (selectedObject instanceof StudentClass) {
			studentClass = (StudentClass) selectedObject;
		} else if (selectedObject instanceof User) {
			studentClass = host.getClassById(((User) selectedObject).getStudentClassId());
		} else {
			return;
		}

		PacketPrintingInformation printingInformation = Printer.getPrintingInformation(host);
		PrintDialog printDialog = new PrintDialog(mainWindow, studentClass, printingInformation.getFooterNote());
		printDialog.setVisible(true);
	}

	// INTERNAL
	private static void applyActions(MainWindow mainWindow, Action... actions) {
		if (actions.length == 0) return;

		Host host = mainWindow.getHost();
		BaseAction[] baseActions = new BaseAction[actions.length];

		for (int i = 0; i < actions.length; ++i) {
			Action action = actions[i];
			if (action == null) continue;

			baseActions[i] = new BaseAction(action, host);
		}

		host.addActions(baseActions);
		mainWindow.updateAll();
	}
}
