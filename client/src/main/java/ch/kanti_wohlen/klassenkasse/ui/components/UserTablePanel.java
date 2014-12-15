package ch.kanti_wohlen.klassenkasse.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterEvent.Type;
import javax.swing.event.RowSorterListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.util.ConditionalFormatting;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.RoleComparator;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class UserTablePanel extends JPanel {

	public enum RoleMode {
		DONT_SHOW,
		SHOW_ICON,
		SHOW_NAME,
		SHOW_BOTH;

		private boolean showName() {
			return this == SHOW_NAME || this == SHOW_BOTH;
		}

		private boolean showIcon() {
			return this == SHOW_ICON || this == SHOW_BOTH;
		}
	}

	private final JTable table;
	private final RoleMode roleMode;
	private final boolean showBalance;

	private List<User> users;

	/**
	 * Create the panel.
	 */
	public UserTablePanel() {
		this(RoleMode.SHOW_ICON, true);
	}

	public UserTablePanel(RoleMode roleMode, boolean showBalance) {
		this.roleMode = roleMode;
		this.showBalance = showBalance;

		setLayout(new BorderLayout());
		setBorder(new JScrollPane().getBorder());

		table = new JTable();
		table.setFocusable(false);
		table.setPreferredScrollableViewportSize(new Dimension(450, 100));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);
		table.setAutoCreateColumnsFromModel(false);
		table.getTableHeader().setReorderingAllowed(false);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));

		buildColumns();

		users = new ArrayList<User>();
		table.setModel(new UneditableDefaultTableModel(table.getColumnCount()));
		setSorter();

		add(table, BorderLayout.CENTER);
		add(table.getTableHeader(), BorderLayout.NORTH);
	}

	private void buildColumns() {
		TableColumnModel columns = new DefaultTableColumnModel();
		columns.setColumnMargin(0);

		if (roleMode != RoleMode.DONT_SHOW) addRoleColumn(columns, roleMode);
		addNameColumn(columns);
		if (showBalance) addBalanceColumn(columns);

		table.setColumnModel(columns);
	}

	private static void addRoleColumn(TableColumnModel model, RoleMode roleMode) {
		TableColumn column = new TableColumn(model.getColumnCount(), 100);
		if (roleMode.showName()) {
			column.setHeaderValue("Benutzerrolle");
			column.setMinWidth(100);
		} else {
			column.setMaxWidth(25);
			column.setHeaderValue("");
			column.setResizable(false);
		}
		column.setCellRenderer(new RoleCellRenderer(roleMode));
		model.addColumn(column);
	}

	private static void addNameColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount());
		column.setHeaderValue("Name");
		column.setMinWidth(150);
		model.addColumn(column);
	}

	private static void addBalanceColumn(TableColumnModel model) {
		TableColumn column = new TableColumn(model.getColumnCount(), 120);
		column.setHeaderValue("Kontostand");
		column.setMinWidth(100);
		column.setMaxWidth(250);
		column.setCellRenderer(new ConditionalFormattingRenderer());
		model.addColumn(column);
	}

	public void loadData(Collection<User> users, @NonNull Host host) {
		List<User> userList = new ArrayList<>();
		DefaultTableModel model = new UneditableDefaultTableModel(table.getColumnCount());

		for (User user : users) {
			if (user == null) continue;

			Vector<Object> row = new Vector<>();
			if (roleMode != RoleMode.DONT_SHOW) row.add(user.getRole(host));
			row.add(user.getFullName());
			if (showBalance) row.add(user.getBalance());

			model.addRow(row);
			userList.add(user);
		}

		table.setModel(model);
		table.setColumnModel(table.getColumnModel());

		setSorter();

		this.users = userList;
	}

	private void setSorter() {
		TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
		if (roleMode != RoleMode.DONT_SHOW) {
			sorter.setComparator(0, new RoleComparator());
			sorter.addRowSorterListener(new RowSorterListener() {

				@Override
				public void sorterChanged(RowSorterEvent e) {
					if (e.getType() != Type.SORT_ORDER_CHANGED) return;

					RowSorter<?> sorter = e.getSource();
					List<? extends SortKey> keys = sorter.getSortKeys();
					if (keys.size() == 0) return;
					if (keys.size() == 2 && keys.get(1).getColumn() != 1) return;

					SortKey first = keys.get(0);
					if (first.getColumn() == 0) {
						sorter.setSortKeys(Arrays.asList(first, new SortKey(1, first.getSortOrder())));
					}
				}
			});
		}

		if (showBalance) {
			int i = roleMode == RoleMode.DONT_SHOW ? 1 : 2;
			sorter.setComparator(i, new MonetaryValueComparator());
		}

		table.setRowSorter(sorter);
		sorter.setSortKeys(Arrays.asList(new SortKey(0, SortOrder.ASCENDING)));
	}

	public JTable getTable() {
		return table;
	}

	public @Nullable User getSelectedUser() {
		if (table.getSelectedRow() < 0) return null;
		int idx = table.getRowSorter().convertRowIndexToModel(table.getSelectedRow());
		return users.get(idx);
	}

	public void addUser(@NonNull User user, @NonNull Host host) {
		users.add(user);

		Vector<Object> row = new Vector<>();
		if (roleMode != RoleMode.DONT_SHOW) row.add(user.getRole(host));
		row.add(user.getFullName());
		if (showBalance) row.add(user.getBalance());

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		model.addRow(row);
	}

	public void selectUser(@Nullable User user) {
		ListSelectionModel selection = table.getSelectionModel();
		if (user == null) {
			selection.clearSelection();
		} else {
			int idx = -1;
			for (int i = 0; i < users.size(); ++i) {
				if (users.get(i).getLocalId() == user.getLocalId()) {
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

	private static class UneditableDefaultTableModel extends DefaultTableModel {

		public UneditableDefaultTableModel(int columnCount) {
			super(0, columnCount);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	}

	private static class RoleCellRenderer extends DefaultTableCellRenderer {

		private final RoleMode roleMode;

		private RoleCellRenderer(RoleMode roleMode) {
			this.roleMode = roleMode;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof Role) {
				Role role = (Role) value;
				if (roleMode.showName()) {
					setText(role.getName());
				} else {
					setText("");
				}
				if (roleMode.showIcon()) {
					setIcon(IconProvider.forRole(role.getLocalId()));
				} else {
					setIcon(null);
				}
			}
			return this;
		}
	}

	private static class ConditionalFormattingRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof MonetaryValue) {
				setForeground(ConditionalFormatting.getUserColor((MonetaryValue) value));
			} else {
				setForeground(Color.BLACK);
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
}
