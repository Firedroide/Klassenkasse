package ch.kanti_wohlen.klassenkasse.ui.util;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;

public class HostTreeCellRenderer implements TreeCellRenderer {

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Object userObject = node.getUserObject();
		JLabel label = new JLabel();
		if (selected) {
			label.setForeground(Color.WHITE);
		}

		if (userObject instanceof Host) {
			label.setText(((Host) node.getUserObject()).getName());
			label.setIcon(IconProvider.HOME);
		} else if (userObject instanceof StudentClass) {
			label.setText(((StudentClass) node.getUserObject()).getName());
			label.setIcon(IconProvider.forStudentClass(!leaf));
		} else if (userObject instanceof User) {
			User user = (User) node.getUserObject();
			label.setText(user.getFullName());
			label.setIcon(IconProvider.forUser(user));
		} else {
			label.setText(tree.convertValueToText(value, selected, expanded, leaf, row, hasFocus));
		}

		return label;
	}
}
