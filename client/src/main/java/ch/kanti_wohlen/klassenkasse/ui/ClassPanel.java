package ch.kanti_wohlen.klassenkasse.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.components.UserTablePanel;
import ch.kanti_wohlen.klassenkasse.ui.components.UserTablePanel.RoleMode;
import ch.kanti_wohlen.klassenkasse.ui.util.ConditionalFormatting;
import ch.kanti_wohlen.klassenkasse.ui.util.FontProvider;
import ch.kanti_wohlen.klassenkasse.ui.util.PopUpMenus;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class ClassPanel extends JPanel {

	private final JLabel titleLabel;
	private final UserTablePanel userTable;
	private JLabel balanceLabel;
	private JLabel usersLabel;
	private JLabel roundingLabel;

	/**
	 * Create the panel.
	 */
	public ClassPanel(final @NonNull MainWindow mainWindow, StudentClass studentClass) {
		setLayout(new MigLayout("", "[grow][grow]", "[][][][][grow]"));

		titleLabel = new JLabel("Klasse ");
		titleLabel.setFont(FontProvider.SUBTITLE_FONT);
		add(titleLabel, "cell 0 0 2 1,gapx 8,gapy 5 15");

		JLabel balanceDescriptionLabel = new JLabel("Klassensaldo:");
		add(balanceDescriptionLabel, "cell 0 1,gapx 8");

		balanceLabel = new JLabel(MonetaryValue.ZERO.toString());
		balanceLabel.setFont(FontProvider.DEFAULT_BOLD);
		balanceLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		add(balanceLabel, "cell 0 1,growx,gapx 0 10");

		JLabel roundingDescriptionValue = new JLabel("Rundungen:");
		add(roundingDescriptionValue, "cell 0 2,gapx 8");

		roundingLabel = new JLabel(MonetaryValue.ZERO.toString());
		roundingLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		add(roundingLabel, "cell 0 2,growx,gapx 0 10");

		JLabel usersDescriptionLabel = new JLabel("Nutzer in dieser Klasse:");
		usersDescriptionLabel.setFont(FontProvider.DEFAULT_BOLD);
		add(usersDescriptionLabel, "cell 0 3,gapx 8");

		usersLabel = new JLabel("0");
		usersLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		add(usersLabel, "cell 0 3,growx,gapx 0 10");

		userTable = new UserTablePanel(RoleMode.SHOW_ICON, true);
		userTable.getTable().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
					User user = userTable.getSelectedUser();
					if (user == null) return;
					mainWindow.displayUser(user);
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					int row = userTable.getTable().rowAtPoint(e.getPoint());
					userTable.getTable().getSelectionModel().setSelectionInterval(row, row);

					User user = userTable.getSelectedUser();
					if (user == null) return;

					JPopupMenu menu = PopUpMenus.popUpMenuForUser(mainWindow, user);
					menu.show(userTable.getTable(), e.getX(), e.getY());
				}
			}
		});
		add(userTable, "cell 0 4 2 1,grow");

		if (studentClass != null) loadData(studentClass, mainWindow.getHost());
	}

	private void loadData(StudentClass studentClass, Host host) {
		titleLabel.setText("Klasse " + studentClass.getName());
		balanceLabel.setText(studentClass.getBalance().toString());
		balanceLabel.setForeground(ConditionalFormatting.getClassColor(studentClass.getBalance()));
		roundingLabel.setText(studentClass.getRoundingValue().toString());

		Collection<User> users = host.getUsersByClass(studentClass.getLocalId()).values();
		usersLabel.setText(String.valueOf(users.size()));
		userTable.loadData(users, host);
	}

	public UserTablePanel getUsersTable() {
		return userTable;
	}
}
