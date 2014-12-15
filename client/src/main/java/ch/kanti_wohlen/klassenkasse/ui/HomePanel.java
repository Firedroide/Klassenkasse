package ch.kanti_wohlen.klassenkasse.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.components.ClassButton;
import ch.kanti_wohlen.klassenkasse.ui.util.FontProvider;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

import com.camick.swing.WrapLayout;

public class HomePanel extends JPanel implements Scrollable {

	private JLabel titleLabel;
	private JPanel buttonPanel;
	private JLabel classesLabel;
	private JLabel balanceDescriptionLabel;
	private JLabel balanceLabel;

	/**
	 * Create the panel.
	 */
	public HomePanel(MainWindow mainWindow) {
		setLayout(new MigLayout("", "[grow]", "[]20[][push,grow,fill][]"));

		titleLabel = new JLabel("Klassenkasse");
		titleLabel.setFont(FontProvider.TITLE_FONT);
		add(titleLabel, "cell 0 0,growx,gapx 8,aligny top");

		buttonPanel = new JPanel();
		buttonPanel.setBackground(new Color(242, 242, 242));
		buttonPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 5, 5));
		buttonPanel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), new EmptyBorder(10, 10, 10, 10)));

		classesLabel = new JLabel("Klassen:");
		classesLabel.setFont(FontProvider.SUBTITLE_FONT);
		add(classesLabel, "cell 0 1,gapx 8,gapy 0 5");

		add(buttonPanel, "cell 0 2,grow");

		balanceDescriptionLabel = new JLabel("Saldo:");
		balanceDescriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
		add(balanceDescriptionLabel, "flowx,cell 0 3,gapx 8,gapy 5");

		balanceLabel = new JLabel(MonetaryValue.ZERO.getAmountString(true));
		balanceLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
		add(balanceLabel, "cell 0 3,gapx 20,gapy 5,grow");

		if (mainWindow != null) loadData(mainWindow);
	}

	private void loadData(@NonNull MainWindow mainWindow) {
		Map<Integer, StudentClass> classes = mainWindow.getHost().getClasses();
		Map<Integer, User> users = mainWindow.getHost().getUsers();

		Map<Integer, Integer> classMap = mapUsersToClasses(users);
		MonetaryValue balance = MonetaryValue.ZERO;

		for (StudentClass studentClass : classes.values()) {
			if (studentClass == null || studentClass.getLocalId() == 0) continue;

			balance = balance.add(studentClass.getRoundedBalance());
			int classId = studentClass.getLocalId();
			int userCount = classMap.containsKey(classId) ? classMap.get(classId) : 0;
			ClassButton classButton = new ClassButton(mainWindow, studentClass, userCount);
			buttonPanel.add(classButton);
		}

		balanceLabel.setText(balance.getAmountString(true));
	}

	private Map<Integer, Integer> mapUsersToClasses(Map<Integer, User> users) {
		Map<Integer, Integer> classMap = new HashMap<>();
		for (User user : users.values()) {
			int classId = user.getStudentClassId();
			if (classMap.containsKey(classId)) {
				classMap.put(classId, classMap.get(classId) + 1);
			} else {
				classMap.put(classId, 1);
			}
		}
		return classMap;
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 32;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 32;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
