package ch.kanti_wohlen.klassenkasse.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class PaymentTablePanel extends JPanel {

	public static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag("de-ch"));

	private final JTable table;
	private List<Payment> payments;

	/**
	 * Create the panel.
	 */
	public PaymentTablePanel() {
		setLayout(new BorderLayout());
		setBorder(new JScrollPane().getBorder());

		table = new JTable();
		table.setFocusable(false);
		table.setPreferredScrollableViewportSize(new Dimension(450, 100));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);
		table.setFillsViewportHeight(true);
		table.setAutoCreateColumnsFromModel(false);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));

		buildColumns();

		add(table, BorderLayout.CENTER);
		add(table.getTableHeader(), BorderLayout.NORTH);

		payments = Collections.emptyList();
	}

	private void buildColumns() {
		TableColumnModel columns = new DefaultTableColumnModel();
		columns.setColumnMargin(0);

		addDateColumn(columns);
		addDescriptionColumn(columns);
		addValueColumn(columns);

		table.setColumnModel(columns);
	}

	private static void addDateColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount(), 100);
		column.setHeaderValue("Datum");
		column.setMaxWidth(80);
		column.setResizable(false);
		column.setCellRenderer(new DateTableCellRenderer());
		model.addColumn(column);
	}

	private static void addDescriptionColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount());
		column.setHeaderValue("Beschreibung");
		column.setMinWidth(150);
		model.addColumn(column);
	}

	private static void addValueColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount());
		column.setHeaderValue("Wert");
		column.setMinWidth(120);
		column.setMaxWidth(250);
		model.addColumn(column);
	}

	public void loadData(Collection<Payment> payments) {
		List<Payment> paymentList = new ArrayList<>();
		DefaultTableModel model = new DefaultTableModel(0, table.getColumnCount()) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		for (Payment payment : payments) {
			if (payment == null) continue;

			Vector<Object> row = new Vector<>();
			row.add(payment.getDate());
			row.add(payment.getDescription());
			row.add(payment.getValue());

			model.addRow(row);
			paymentList.add(payment);
		}

		table.setModel(model);
		table.setColumnModel(table.getColumnModel());

		TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
		sorter.setComparator(0, new DateComparator());
		sorter.setComparator(2, new MonetaryValueComparator());
		table.setRowSorter(sorter);
		sorter.setSortKeys(Arrays.asList(new SortKey(0, SortOrder.DESCENDING)));

		this.payments = paymentList;
	}

	public JTable getTable() {
		return table;
	}

	public @Nullable Payment getSelectedPayment() {
		if (table.getSelectedRow() < 0) return null;
		int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
		return payments.get(idx);
	}

	public void selectPayment(@Nullable Payment payment) {
		ListSelectionModel selection = table.getSelectionModel();
		if (payment == null) {
			selection.clearSelection();
		} else {
			int idx = -1;
			for (int i = 0; i < payments.size(); ++i) {
				if (payments.get(i).getLocalId() == payment.getLocalId()) {
					idx = i;
					break;
				}
			}

			if (idx < 0) {
				selection.clearSelection();
			} else {
				selection.setSelectionInterval(idx, idx);
			}
		}
	}

	private static class DateTableCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof Date) {
				setText(DATE_FORMAT.format(value));
			}

			return this;
		}
	}

	private static class MonetaryValueComparator implements Comparator<MonetaryValue> {

		@Override
		public int compare(MonetaryValue o1, MonetaryValue o2) {
			return Long.compare(o1.getCentValue(), o2.getCentValue());
		}
	}

	private static class DateComparator implements Comparator<Date> {

		@Override
		public int compare(Date o1, Date o2) {
			return o1.compareTo(o2);
		}
	}
}
