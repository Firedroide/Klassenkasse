package ch.kanti_wohlen.klassenkasse.ui.components;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class CheckboxCellRenderer implements TableCellRenderer {

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (value == null) return null;
		if (!(value instanceof JCheckBox)) {
			throw new IllegalArgumentException("Row " + row + ", Column " + column + " is not a JCheckBox.");
		}

		JCheckBox cBox = (JCheckBox) value;
		cBox.setSelected(isSelected);
		return cBox;
	}
}
