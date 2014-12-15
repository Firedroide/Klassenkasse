package ch.kanti_wohlen.klassenkasse.ui.components;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class IconCellRenderer implements TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (value == null) return null;
		if (!(value instanceof Icon)) {
			throw new IllegalArgumentException("Row " + row + ", Column " + column + " is not an Icon.");
		}

		return new JLabel((Icon) value);
	}
}
