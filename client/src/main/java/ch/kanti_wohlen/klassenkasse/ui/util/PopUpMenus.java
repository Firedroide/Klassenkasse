package ch.kanti_wohlen.klassenkasse.ui.util;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.printing.Printer;
import ch.kanti_wohlen.klassenkasse.ui.ClassPanel;
import ch.kanti_wohlen.klassenkasse.ui.MainWindow;
import ch.kanti_wohlen.klassenkasse.ui.dialog.Dialogs;
import ch.kanti_wohlen.klassenkasse.util.PermissionsHelper;

public final class PopUpMenus {

	private PopUpMenus() {}

	public static JPopupMenu popUpMenuForClass(final @NonNull MainWindow mainWindow, final @NonNull StudentClass studentClass) {
		JPopupMenu menu = new JPopupMenu();

		JMenuItem display = new JMenuItem("Detailansicht", IconProvider.DETAILS);
		display.setFont(display.getFont().deriveFont(Font.BOLD));
		display.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.displayClass(studentClass);
			}
		});
		menu.add(display);

		menu.addSeparator();

		JMenuItem edit = new JMenuItem("Klasse bearbeiten", IconProvider.EDIT);
		edit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.editClass(mainWindow, studentClass);
			}
		});
		edit.setEnabled(PermissionsHelper.canUpdateClass(mainWindow.getHost(), studentClass, UpdateType.UPDATE));
		menu.add(edit);

		JMenuItem delete = new JMenuItem("Klasse löschen", IconProvider.DELETE);
		delete.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.deleteClass(mainWindow, studentClass);
			}
		});
		delete.setEnabled(PermissionsHelper.canUpdateClass(mainWindow.getHost(), studentClass, UpdateType.REMOVAL));
		menu.add(delete);

		menu.addSeparator();

		JMenuItem printClass = new JMenuItem("Klassenübersicht drucken", IconProvider.PRINT);
		printClass.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Printer.printStudentClass(mainWindow, studentClass);
			}
		});
		menu.add(printClass);

		JMenuItem printAllUsers = new JMenuItem("Alle Nutzerberichte drucken", IconProvider.CLASS);
		printAllUsers.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Printer.printAllUsers(mainWindow, studentClass);
			}
		});
		menu.add(printAllUsers);

		return menu;
	}

	public static JPopupMenu popUpMenuForUser(final @NonNull MainWindow mainWindow, final @NonNull User user) {
		JPopupMenu menu = new JPopupMenu();

		JMenuItem display = new JMenuItem("Detailansicht", IconProvider.DETAILS);
		display.setFont(display.getFont().deriveFont(Font.BOLD));
		display.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.displayUser(user);
			}
		});
		menu.add(display);

		menu.addSeparator();

		JMenuItem edit = new JMenuItem("Nutzer bearbeiten", IconProvider.EDIT);
		edit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.editUser(mainWindow, user);
				selectUser(mainWindow, user);
			}
		});
		edit.setEnabled(PermissionsHelper.canUpdateUser(mainWindow.getHost(), user, UpdateType.UPDATE));
		menu.add(edit);

		JMenuItem delete = new JMenuItem("Nutzer löschen", IconProvider.DELETE);
		delete.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.deleteUser(mainWindow, user);
			}
		});
		delete.setEnabled(PermissionsHelper.canUpdateUser(mainWindow.getHost(), user, UpdateType.REMOVAL));
		menu.add(delete);

		menu.addSeparator();

		JMenuItem print = new JMenuItem("Bericht drucken", IconProvider.PRINT);
		print.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Printer.printUser(mainWindow, user);
			}
		});
		menu.add(print);

		JMenuItem payment = new JMenuItem("Zahlung hinzufügen", IconProvider.PAYMENT_ADD);
		payment.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.newPayment(mainWindow, user);
				selectUser(mainWindow, user);
			}
		});
		payment.setEnabled(PermissionsHelper.hasPermission(mainWindow.getHost(), "create.payment.class"));
		menu.add(payment);

		return menu;
	}

	private static void selectUser(MainWindow mainWindow, User user) {
		JPanel contentPanel = mainWindow.getContentPanel();
		if (contentPanel instanceof ClassPanel) {
			ClassPanel classPanel = (ClassPanel) contentPanel;
			classPanel.getUsersTable().selectUser(user);
		}
	}
}
