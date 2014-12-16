package ch.kanti_wohlen.klassenkasse.ui.components;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.ui.MainWindow;
import ch.kanti_wohlen.klassenkasse.ui.util.ConditionalFormatting;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class ClassButton extends JButton {

	public ClassButton(final MainWindow mainWindow, final StudentClass studentClass, int userCount) {
		super();

		setLayout(new BorderLayout());

		Font titleFont = getFont().deriveFont(15.0f);
		JLabel titleLabel = new JLabel(studentClass.getName());
		titleLabel.setFont(titleFont);
		titleLabel.setForeground(getForeground());
		titleLabel.setLabelFor(this);
		add(titleLabel, BorderLayout.CENTER);

		JPanel informationPanel = new JPanel();
		informationPanel.setOpaque(false);
		informationPanel.setLayout(new MigLayout("novisualpadding,gap 0 0,insets 0 0", "[]8[]", "[][]"));

		JLabel usersLabel = new JLabel();
		usersLabel.setText("Nutzer:");
		informationPanel.add(usersLabel, "cell 0 0");

		JLabel userValueLabel = new JLabel(String.valueOf(userCount));
		informationPanel.add(userValueLabel, "cell 1 0,alignx right");

		MonetaryValue balance = studentClass.getRoundedBalance();
		JLabel balanceLabel = new JLabel("Saldo:");
		informationPanel.add(balanceLabel, "cell 0 1");

		JLabel balanceValueLabel = new JLabel(balance.getAmountString(true));
		balanceValueLabel.setForeground(ConditionalFormatting.getClassColor(balance));
		informationPanel.add(balanceValueLabel, "cell 1 1");
		add(informationPanel, BorderLayout.SOUTH);

		addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.displayClass(studentClass);
			}
		});
	}
}
