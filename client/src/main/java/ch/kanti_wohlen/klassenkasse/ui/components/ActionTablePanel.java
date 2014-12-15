package ch.kanti_wohlen.klassenkasse.ui.components;

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
import java.util.Map;
import java.util.Vector;

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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.UserComparator;

public class ActionTablePanel extends JScrollPane {

	public static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag("de-ch"));

	private final JTable table;
	private Map<Integer, User> users;
	private List<BaseAction> actions;

	/**
	 * Create the panel.
	 */
	public ActionTablePanel() {
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

		setViewportView(table);
		setColumnHeaderView(table.getTableHeader());

		actions = Collections.emptyList();
	}

	public void setUsers(Map<Integer, User> users) {
		this.users = users;
	}

	private void buildColumns() {
		TableColumnModel columns = new DefaultTableColumnModel();
		columns.setColumnMargin(0);

		addDateColumn(columns);
		addCreatorColumn(columns);
		addDescriptionColumn(columns);
		addAppliedStateColumn(columns);

		table.setColumnModel(columns);
	}

	private static void addDateColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount(), 80);
		column.setHeaderValue("Datum");
		column.setMinWidth(80);
		column.setMaxWidth(80);
		column.setResizable(false);
		column.setCellRenderer(new DateTableCellRenderer());
		model.addColumn(column);
	}

	private static void addCreatorColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount());
		column.setHeaderValue("Nutzer");
		column.setMinWidth(120);
		column.setCellRenderer(new CreatorCellRenderer());
		model.addColumn(column);
	}

	private static void addDescriptionColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount());
		column.setHeaderValue("Beschreibung");
		column.setMinWidth(150);
		model.addColumn(column);
	}

	private static void addAppliedStateColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount(), 100);
		column.setHeaderValue("Rückgängig");
		column.setMinWidth(100);
		column.setMaxWidth(100);
		column.setResizable(false);
		column.setCellRenderer(new AppliedStateCellRenderer());
		model.addColumn(column);
	}

	public void loadData(@NonNull Host host, Collection<BaseAction> actions) {
		List<BaseAction> actionList = new ArrayList<>();
		DefaultTableModel model = new DefaultTableModel(0, table.getColumnCount()) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		for (BaseAction action : actions) {
			if (action == null) continue;
			User creator = users.get(action.getCreatorId());
			if (creator == null) continue;

			Vector<Object> row = new Vector<>();
			row.add(action.getCreationDate());
			row.add(creator);
			row.add(Action.getActionDescription(host, action));
			row.add(action.isUndone());

			model.addRow(row);
			actionList.add(action);
		}

		table.setModel(model);
		table.setColumnModel(table.getColumnModel());

		TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
		sorter.setComparator(0, new DateComparator());
		sorter.setComparator(1, new UserComparator());
		table.setRowSorter(sorter);
		sorter.setSortKeys(Arrays.asList(new SortKey(0, SortOrder.DESCENDING)));

		this.actions = actionList;
	}

	public JTable getTable() {
		return table;
	}

	public @Nullable BaseAction getSelectedAction() {
		if (table.getSelectedRow() < 0) return null;
		int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
		return actions.get(idx);
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

	private static class CreatorCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof User) {
				User user = (User) value;
				setText(user.getFullName());
				setIcon(IconProvider.forRole(user.getRoleId()));
			}

			return this;
		}
	}

	private static class AppliedStateCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof Boolean) {
				boolean undone = (boolean) value;
				if (undone) {
					setText("Ja");
				} else {
					setText("Nein");
				}
			}

			return this;
		}
	}

	private static class DateComparator implements Comparator<Date> {

		@Override
		public int compare(Date o1, Date o2) {
			return o1.compareTo(o2);
		}
	}
}
