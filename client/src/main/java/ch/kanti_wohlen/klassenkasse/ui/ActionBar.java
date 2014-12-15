package ch.kanti_wohlen.klassenkasse.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;
import org.gpl.JSplitButton.JSplitButton;
import org.gpl.JSplitButton.action.SplitButtonActionListener;

import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.ActionHistory;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.RemoteHost;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.printing.Printer;
import ch.kanti_wohlen.klassenkasse.ui.components.HistoryButton;
import ch.kanti_wohlen.klassenkasse.ui.components.HistoryButton.HistoryListener;
import ch.kanti_wohlen.klassenkasse.ui.dialog.Dialogs;
import ch.kanti_wohlen.klassenkasse.ui.dialog.PreferencesDialog;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.util.PermissionsHelper;

public class ActionBar extends JPanel {

	private final @NonNull MainWindow mainWindow;

	private final @NonNull JSplitButton classAddButton;
	private final @NonNull JPanel dataPanel;
	private final @NonNull JButton userAddButton;
	private final @NonNull JButton paymentAddButton;
	private final @NonNull JButton editButton;
	private final @NonNull JSplitButton printButton;
	private final @NonNull HistoryButton undoButton;
	private final @NonNull JPanel actionPanel;
	private final @NonNull HistoryButton redoButton;
	private final @NonNull JButton serverLogButton;
	private final @NonNull JPanel settingsPanel;
	private final @NonNull JButton preferencesButton;
	private JButton printOptionsButton;

	public enum DataType {
		CLASSES,
		USERS,
		PAYMENTS
	}

	/**
	 * Create the panel.
	 */
	public ActionBar(final @NonNull MainWindow mainWindow) {
		this.mainWindow = mainWindow;

		Color lightBackground = new Color(232, 234, 238);
		Color fontColor = new Color(102, 102, 102);
		setBackground(lightBackground);
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setLayout(new MigLayout("gap 0 0,insets 2", "[][][]", "[]"));

		dataPanel = new JPanel();
		dataPanel.setLayout(new MigLayout("gapy 0, insets -8 -5 -1 -3", "[][][fill]", "[][][]"));
		dataPanel.setBackground(lightBackground);
		dataPanel.setBorder(new TitledBorder(null, "Daten", TitledBorder.CENTER, TitledBorder.ABOVE_BOTTOM, null,
				fontColor));
		add(dataPanel, "cell 0 0,alignx left,growy");

		classAddButton = new JSplitButton("Klasse hinzufügen");
		classAddButton.setIcon(IconProvider.CLASS_ADD);
		classAddButton.setIconTextGap(10);
		classAddButton.addSplitButtonActionListener(new SplitButtonActionListener() {

			@Override
			public void buttonClicked(ActionEvent e) {
				Dialogs.newClass(mainWindow);
			}

			@Override
			public void splitButtonClicked(ActionEvent e) {}
		});

		JPopupMenu classMenu = new JPopupMenu();
		classMenu.add(new JMenuItem("Aus altem Format importieren", IconProvider.DISK)).addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				OldFormatImporter.importFromOldFormat(mainWindow);
			}
		});
		classAddButton.setPopupMenu(classMenu);
		dataPanel.add(classAddButton, "flowy,cell 2 0,alignx left,aligny top");

		editButton = new JButton("Bearbeiten");
		editButton.setVerticalAlignment(SwingConstants.BOTTOM);
		editButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		editButton.setHorizontalTextPosition(SwingConstants.CENTER);
		editButton.setIcon(IconProvider.EDIT_LARGE);
		editButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object selected = mainWindow.getSelectedObject();
				if (selected instanceof StudentClass) {
					Dialogs.editClass(mainWindow, (StudentClass) selected);
				} else if (selected instanceof User) {
					Dialogs.editUser(mainWindow, (User) selected);
				}
			}
		});
		dataPanel.add(editButton, "cell 1 0 1 3,grow");

		printButton = new JSplitButton("Drucken");
		printButton.setIcon(IconProvider.PRINT_LARGE);
		printButton.setVerticalAlignment(SwingConstants.BOTTOM);
		printButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		printButton.setHorizontalTextPosition(SwingConstants.CENTER);
		printButton.addSplitButtonActionListener(new SplitButtonActionListener() {

			@Override
			public void buttonClicked(ActionEvent e) {
				Object selected = mainWindow.getSelectedObject();
				if (selected instanceof StudentClass) {
					Printer.printStudentClass(mainWindow, (StudentClass) selected);
				} else if (selected instanceof User) {
					Printer.printUser(mainWindow, (User) selected);
				}
			}

			@Override
			public void splitButtonClicked(ActionEvent e) {}
		});

		JPopupMenu printMenu = new JPopupMenu();
		printMenu.add(new JMenuItem("Alle Klassenübersichten drucken", IconProvider.CLASS)).addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Printer.printAllClasses(mainWindow);
			}
		});

		printMenu.add(new JMenuItem("Alle Nutzerberichte drucken", IconProvider.USER)).addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object selectedObject = mainWindow.getSelectedObject();
				if (!(selectedObject instanceof StudentClass)) return;

				Printer.printAllUsers(mainWindow, (StudentClass) selectedObject);
			}
		});

		printButton.setPopupMenu(printMenu);
		dataPanel.add(printButton, "cell 0 0 1 3,grow");

		userAddButton = new JButton("Nutzer hinzufügen");
		userAddButton.setHorizontalAlignment(SwingConstants.LEFT);
		userAddButton.setIconTextGap(15);
		userAddButton.setIcon(IconProvider.USER_ADD);
		userAddButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object selected = mainWindow.getSelectedObject();
				if (selected instanceof StudentClass) {
					Dialogs.newUser(mainWindow, (StudentClass) selected);
				} else if (selected instanceof User) {
					StudentClass studentClass = ((User) selected).getStudentClass(mainWindow.getHost());
					Dialogs.newUser(mainWindow, studentClass);
				} else {
					Dialogs.newUser(mainWindow);
				}
			}
		});
		dataPanel.add(userAddButton, "cell 2 1,alignx left,aligny top");

		paymentAddButton = new JButton("Zahlung hinzufügen");
		paymentAddButton.setHorizontalAlignment(SwingConstants.LEFT);
		paymentAddButton.setIconTextGap(10);
		paymentAddButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Object selected = mainWindow.getSelectedObject();
				if (selected instanceof StudentClass) {
					Dialogs.newPayment(mainWindow, (StudentClass) selected);
				} else if (selected instanceof User) {
					Dialogs.newPayment(mainWindow, (User) selected);
				} else {
					Dialogs.newPayment(mainWindow);
				}
			}
		});
		paymentAddButton.setIcon(IconProvider.PAYMENT_ADD);
		dataPanel.add(paymentAddButton, "cell 2 2");

		actionPanel = new JPanel();
		actionPanel.setLayout(new MigLayout("gapy 2,gapx 2, insets -8 -5 -1 -3", "[][]", "[][][]"));
		actionPanel.setBackground(lightBackground);
		actionPanel.setBorder(new TitledBorder(null, "Aktionen", TitledBorder.CENTER, TitledBorder.ABOVE_BOTTOM, null,
				fontColor));
		add(actionPanel, "cell 1 0,alignx left,growy");

		final RemoteHost remoteHost = (RemoteHost) mainWindow.getHost();
		final ActionHistory history = remoteHost.getActionHistory();
		undoButton = new HistoryButton(remoteHost, history.getUndoableActions(), new HistoryListener() {

			@Override
			public void historyChanged(Collection<BaseAction[]> changedActions) {
				for (BaseAction[] actionBunch : changedActions) {
					if (actionBunch == null || actionBunch.length == 0) continue;

					remoteHost.setActionsUndone(true, actionBunch);
					history.getRedoableActions().add(actionBunch);
				}

				mainWindow.updateAll();
			}
		});
		undoButton.setText("Rückgängig");
		undoButton.setIcon(IconProvider.UNDO);
		actionPanel.add(undoButton, "flowx,cell 0 0,grow");

		redoButton = new HistoryButton(remoteHost, history.getRedoableActions(), new HistoryListener() {

			@Override
			public void historyChanged(Collection<BaseAction[]> changedActions) {
				for (BaseAction[] actionBunch : changedActions) {
					if (actionBunch == null || actionBunch.length == 0) continue;

					remoteHost.setActionsUndone(false, actionBunch);
					history.getUndoableActions().add(actionBunch);
				}

				mainWindow.updateAll();
			}
		});
		redoButton.setMargin(new Insets(0, -5, 0, 0));
		redoButton.setText("");
		redoButton.setIcon(IconProvider.REDO);
		actionPanel.add(redoButton, "cell 1 0,grow");

		serverLogButton = new JButton("Server-Log", IconProvider.LOG);
		serverLogButton.setIconTextGap(10);
		serverLogButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.displayHistory();
			}
		});
		actionPanel.add(serverLogButton, "cell 0 1 2 1,growx");

		settingsPanel = new JPanel();
		settingsPanel.setLayout(new MigLayout("gapy 0,insets -8 -5 -1 -3", "[fill]", "[][]"));
		settingsPanel.setBackground(lightBackground);
		settingsPanel.setBorder(new TitledBorder(null, "Optionen", TitledBorder.CENTER, TitledBorder.ABOVE_BOTTOM,
				null, new Color(102, 102, 102)));
		add(settingsPanel, "cell 2 0,grow");

		preferencesButton = new JButton("Einstellungen", IconProvider.SETTINGS);
		preferencesButton.setVerticalAlignment(SwingConstants.BOTTOM);
		preferencesButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				PreferencesDialog dialog = new PreferencesDialog(mainWindow);
				dialog.setVisible(true);
				mainWindow.updateAll();
			}
		});
		settingsPanel.add(preferencesButton, "cell 0 0");

		printOptionsButton = new JButton("Druckoptionen", IconProvider.PRINT);
		printOptionsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Dialogs.showPrintDialog(mainWindow);
			}
		});
		settingsPanel.add(printOptionsButton, "cell 0 1");

		update();
	}

	public void update() {
		Host host = mainWindow.getHost();
		Object selectedObject = mainWindow.getSelectedObject();

		boolean isPrintable;
		boolean canEditValue;
		boolean canEditPrint;
		if (selectedObject instanceof StudentClass) {
			isPrintable = true;
			canEditValue = PermissionsHelper.canUpdateClass(host, (StudentClass) selectedObject, UpdateType.UPDATE);
			canEditPrint = canEditValue;
		} else if (selectedObject instanceof User) {
			User user = (User) selectedObject;
			isPrintable = true;
			canEditValue = PermissionsHelper.canUpdateUser(host, user, UpdateType.UPDATE);
			canEditPrint = PermissionsHelper.canUpdateClass(host, user.getStudentClassId(), UpdateType.UPDATE);
		} else {
			isPrintable = false;
			canEditValue = false;
			canEditPrint = false;
		}

		printButton.setEnabled(isPrintable);
		editButton.setEnabled(canEditValue);
		printOptionsButton.setEnabled(canEditPrint);

		classAddButton.setEnabled(PermissionsHelper.hasPermission(host, "create.class"));
		userAddButton.setEnabled(PermissionsHelper.hasPermission(host, "create.user.class"));
		paymentAddButton.setEnabled(PermissionsHelper.hasPermission(host, "create.payment.class"));

		serverLogButton.setEnabled(PermissionsHelper.hasPermission(host, "edit.history"));
	}

	public void setDisplayedDataType(DataType dataType) {
		switch (dataType) {
		case CLASSES:
			break;
		case PAYMENTS:
			break;
		case USERS:
			break;
		default:
			throw new IllegalArgumentException("Unknown data type");
		}
	}
}
