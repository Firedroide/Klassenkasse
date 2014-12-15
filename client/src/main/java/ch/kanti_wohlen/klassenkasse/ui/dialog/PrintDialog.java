package ch.kanti_wohlen.klassenkasse.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.ui.MainWindow;
import ch.kanti_wohlen.klassenkasse.util.Configuration;

import javax.swing.BoxLayout;

public class PrintDialog extends JDialog implements ActionListener {

	private static final @NonNull String[] COLUMN_HEADERS = {"Variable", "Wert"};

	private final MainWindow mainWindow;
	private final Host host;
	private final StudentClass studentClass;
	private final Map<String, String> currentVariables;
	private final String footer;

	private final JPanel contentPanel;
	private final JTable variablesTable;
	private final JTextPane previewTextPane;

	private Map<String, String> generatedVariables;
	private JCheckBox enabledCheckBox;
	private JCheckBox onePageCheckBox;

	/**
	 * Create the dialog.
	 */
	public PrintDialog(MainWindow mainWindow, StudentClass studentClass, String footer) {
		this.mainWindow = mainWindow;
		this.host = mainWindow.getHost();
		this.studentClass = studentClass;
		this.currentVariables = new HashMap<>();
		this.generatedVariables = Collections.emptyMap();
		this.footer = footer;

		setResizable(false);
		setBounds(100, 100, 450, 464);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle("Druckoptionen");
		setMinimumSize(new Dimension(300, 350));

		contentPanel = new JPanel();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JPanel previewPanel = new JPanel();
			previewPanel.setBorder(new EmptyBorder(3, 3, 1, 3));
			contentPanel.add(previewPanel, BorderLayout.SOUTH);
			previewPanel.setLayout(new BorderLayout(0, 0));
			{
				previewTextPane = new JTextPane();
				previewTextPane.setContentType("text/html");
				previewTextPane.setFocusable(false);
				previewTextPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				previewPanel.add(previewTextPane);
				previewTextPane.setEditable(false);
			}
			{
				JLabel previewLabel = new JLabel("<HTML><b>Vorschau:</b></HTML>");
				previewLabel.setBorder(new EmptyBorder(1, 3, 1, 0));
				previewPanel.add(previewLabel, BorderLayout.NORTH);
			}
		}
		{
			JScrollPane scrollPane = new JScrollPane();
			contentPanel.add(scrollPane, BorderLayout.CENTER);
			{
				variablesTable = new JTable();
				variablesTable.setFillsViewportHeight(true);
				variablesTable.setDefaultEditor(Object.class, new MultilineTableEditor(variablesTable));
				variablesTable.setModel(new DefaultTableModel(COLUMN_HEADERS, 0));
				variablesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				variablesTable.getTableHeader().setReorderingAllowed(false);

				TableColumnModel columnModel = variablesTable.getColumnModel();
				columnModel.getColumn(0).setMinWidth(120);
				columnModel.getColumn(1).setMinWidth(150);

				scrollPane.setViewportView(variablesTable);
			}
		}
		{
			JPanel checkBoxPanel = new JPanel();
			getContentPane().add(checkBoxPanel, BorderLayout.NORTH);
			checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
			{
				onePageCheckBox = new JCheckBox("Länge eines Nutzerberichts auf maximal eine Seite beschränken");
				onePageCheckBox.setSelected(mainWindow.getPreferences().getBoolean("printOnePagePerUser"));
				onePageCheckBox.setBorder(new EmptyBorder(12, 10, 4, 10));
				checkBoxPanel.add(onePageCheckBox);
			}
			{
				enabledCheckBox = new JCheckBox("Fussnote drucken");
				checkBoxPanel.add(enabledCheckBox);
				enabledCheckBox.setSelected(mainWindow.getPreferences().getBoolean("printUserFooterNote"));
				contentPanel.setEnabled(enabledCheckBox.isSelected());
				enabledCheckBox.setBorder(new EmptyBorder(4, 10, 8, 10));
				enabledCheckBox.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						contentPanel.setEnabled(enabledCheckBox.isSelected());
					}
				});
			}
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Abbrechen");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(this);
				buttonPane.add(cancelButton);
			}
		}

		if (host != null && studentClass != null && footer != null) {
			loadData(host, studentClass, footer);
		}
	}

	@NonNullByDefault
	private void loadData(Host host, StudentClass studentClass, String footer) {
		currentVariables.clear();
		generatedVariables = host.getPrintingVariablesForClass(studentClass.getLocalId());
		currentVariables.putAll(generatedVariables);

		// Add any user variables that have not been set yet, but which exist in the footer
		Matcher userVarMatcher = Pattern.compile("\\{.+}").matcher(footer);
		while (userVarMatcher.find()) {
			String var = userVarMatcher.group();
			var = var.substring(1, var.length() - 1); // Shave off the braces

			if (!currentVariables.containsKey(var)) {
				currentVariables.put(var, "");
			}
		}

		DefaultTableModel model = new DefaultTableModel(COLUMN_HEADERS, 0) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return (column == 0) ? false : true;
			}
		};

		for (Entry<String, String> entry : currentVariables.entrySet()) {
			model.addRow(new String[] {entry.getKey(), entry.getValue()});
		}
		variablesTable.setModel(model);

		TableColumnModel columnModel = variablesTable.getColumnModel();
		columnModel.getColumn(0).setMinWidth(100);
		columnModel.getColumn(1).setMinWidth(150);

		TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
		sorter.setSortable(1, false);
		variablesTable.setRowSorter(sorter);

		model.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(@Nullable TableModelEvent e) {
				if (e == null) return;
				if (e.getType() != TableModelEvent.UPDATE) return;
				int row = e.getFirstRow();

				String key = String.valueOf(variablesTable.getModel().getValueAt(row, 0));
				String newValue = String.valueOf(variablesTable.getModel().getValueAt(row, 1));

				currentVariables.put(key, newValue);
				updatePreview();
			}
		});

		updatePreview();
	}

	@SuppressWarnings("null")
	@Override
	public void actionPerformed(ActionEvent e) {
		if ("OK".equals(e.getActionCommand())) {
			generatedVariables = Collections.unmodifiableMap(currentVariables);
			host.updatePrintingVariablesForClass(studentClass.getLocalId(), currentVariables);
			Configuration preferences = mainWindow.getPreferences();
			preferences.set("printUserFooterNote", enabledCheckBox.isSelected());
			preferences.set("printOnePagePerUser", onePageCheckBox.isSelected());
		}

		dispose();
	}

	/**
	 * Gets the printing variables generated by this dialog.
	 * <p>
	 * If the user changed the variables and pressed {@code OK}, this will return these variables.
	 * If the user cancelled the editing, this will return the original values provided by the {@link Host}.
	 * </p>
	 * 
	 * @return a {@link Map} containing all printing variables for this {@link StudentClass}
	 */
	public @NonNull Map<String, String> getGeneratedVariables() {
		if (generatedVariables != null) {
			return generatedVariables;
		} else {
			return new HashMap<>();
		}
	}

	/**
	 * Updates {@code previewTextPane} to show the footer with all variables of {@code currentVariables} replaced by
	 * their values.
	 */
	private void updatePreview() {
		String preview = this.footer;

		// Replace user variables
		preview = preview.replace("[firstname]", "Vorname").replace("[lastname]", "Nachname")
				.replace("[username]", "Benutzername").replace("[balance]", "Kontostand");

		// Replace user variables
		for (Entry<String, String> entry : currentVariables.entrySet()) {
			String value = entry.getValue();
			if (value == null || value.isEmpty()) continue;

			String var = "{" + entry.getKey() + "}";
			preview = preview.replace(var, value);
		}

		previewTextPane.setText(preview);
	}

	private static class MultilineTableEditor extends AbstractCellEditor implements TableCellEditor {

		private final JTextArea editorComponent;
		private final JScrollPane scrollPane;

		public MultilineTableEditor(final JTable table) {
			this.editorComponent = new JTextArea();
			scrollPane = new JScrollPane(editorComponent);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setBorder(null);

			editorComponent.setBorder(new EmptyBorder(0, 5, 0, 3));
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			editorComponent.setText(String.valueOf(value));
			editorComponent.setCaretPosition(0);

			Rectangle cellBox = table.getCellRect(row, column, false);
			scrollPane.setPreferredSize(cellBox.getSize());

			return scrollPane;
		}

		@Override
		public Object getCellEditorValue() {
			return editorComponent.getText();
		}

		@Override
		public boolean stopCellEditing() {
			return super.stopCellEditing();
		}
	}
}
