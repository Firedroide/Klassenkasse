package ch.kanti_wohlen.klassenkasse.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.components.SearchTextField;
import ch.kanti_wohlen.klassenkasse.ui.util.HostTreeCellRenderer;
import ch.kanti_wohlen.klassenkasse.ui.util.PopUpMenus;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.UserComparator;
import ch.kanti_wohlen.klassenkasse.util.PermissionsHelper;

public class NavigationBar extends JPanel {

	private final Map<Integer, TreeNode> studentClasses;
	private final Map<Integer, TreeNode> users;

	private JTextField searchTextField;
	private JTree tree;
	private JScrollPane scrollPane;

	private NavigationSelectionListener selectionListener;
	private DefaultTreeModel sourceModel;
	private Object selectedObject;

	/**
	 * Create the panel.
	 */
	public NavigationBar(@NonNull MainWindow mainWindow) {
		studentClasses = new HashMap<>();
		users = new HashMap<>();

		setLayout(new BorderLayout(0, -2));

		searchTextField = new SearchTextField();
		searchTextField.getDocument().addUndoableEditListener(new UndoableEditListener() {

			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
				boolean isEmpty = searchTextField.getText().isEmpty();
				FilteredTreeModel model = (FilteredTreeModel) tree.getModel();
				model.rebuild();

				TreeNode rootNode = model.getRoot();
				for (int i = 0; i < rootNode.getChildCount(); ++i) {
					TreeNode node = rootNode.getChildAt(i);
					TreePath path = new TreePath(new Object[] {rootNode, node});

					if (isEmpty) {
						tree.collapsePath(path);
					} else {
						tree.expandPath(path);
					}
				}

				TreePath pathToSelected = getUserObjectTreePath(selectedObject);
				tree.setSelectionPath(pathToSelected);
			}
		});
		add(searchTextField, BorderLayout.NORTH);

		scrollPane = new JScrollPane();
		scrollPane.setBackground(Color.WHITE);
		scrollPane.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		add(scrollPane, BorderLayout.CENTER);

		tree = new JTree();
		tree.setToggleClickCount(2);
		sourceModel = new DefaultTreeModel(null);
		tree.setModel(sourceModel);
		tree.setCellRenderer(new HostTreeCellRenderer());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		selectionListener = new NavigationSelectionListener(mainWindow);
		tree.setRootVisible(PermissionsHelper.hasPermission(mainWindow.getHost(), "view.class"));
		tree.addTreeSelectionListener(selectionListener);
		tree.addMouseListener(new ContextMenuProvider(mainWindow));

		scrollPane.setViewportView(tree);

		loadData(mainWindow.getHost());
	}

	public void loadData(Host host) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(host);
		studentClasses.clear();
		users.clear();

		// Classes
		List<StudentClass> classes = new ArrayList<>(host.getClasses().values());

		Collections.sort(classes, new Comparator<StudentClass>() {

			@Override
			public int compare(StudentClass o1, StudentClass o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		for (StudentClass studentClass : classes) {
			int classId = studentClass.getLocalId();
			DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(studentClass);
			this.studentClasses.put(classId, classNode);

			// Users
			List<User> users = new ArrayList<>(host.getUsersByClass(classId).values());
			Collections.sort(users, new UserComparator());

			for (User user : users) {
				int userId = user.getLocalId();
				DefaultMutableTreeNode userNode = new DefaultMutableTreeNode(user);
				this.users.put(userId, userNode);

				classNode.add(userNode);
			}
			root.add(classNode);
		}

		// TODO: Keep all expanded elements expanded?
		sourceModel = new DefaultTreeModel(root);
		tree.setModel(new FilteredTreeModel(sourceModel, searchTextField));

		// If there's only one class, expand it
		if (classes.size() == 1) {
			int row = tree.isRootVisible() ? 1 : 0;
			tree.expandRow(row);
		}

		// For when using this method to update the values
		tree.setSelectionPath(getUserObjectTreePath(selectedObject));
		tree.expandPath(tree.getSelectionPath());
	}

	public void deselect() {
		tree.setSelectionPath(null);
		selectedObject = null;
	}

	public void selectHome() {
		tree.setSelectionRow(0);
	}

	public void selectClass(StudentClass studentClass) {
		tree.setSelectionPath(getUserObjectTreePath(studentClass));
	}

	public void selectUser(User user) {
		tree.setSelectionPath(getUserObjectTreePath(user));
	}

	private TreePath getUserObjectTreePath(Object o) {
		FilteredTreeModel model = (FilteredTreeModel) tree.getModel();
		TreeNode root = model.getRoot();
		selectedObject = o;

		if (o instanceof Host) {
			return new TreePath(root);
		} else if (o instanceof StudentClass) {
			StudentClass studentClass = (StudentClass) o;
			TreeNode node = model.getTransformationOf(studentClasses.get(studentClass.getLocalId()));
			if (node != null) {
				return new TreePath(new Object[] {root, node});
			}
		} else if (o instanceof User) {
			User user = (User) o;
			TreeNode classNode = model.getTransformationOf(studentClasses.get(user.getStudentClassId()));
			TreeNode userNode = model.getTransformationOf(users.get(user.getLocalId()));
			if (classNode != null && userNode != null && userNode.getParent() == classNode) {
				return new TreePath(new Object[] {root, classNode, userNode});
			}
		}

		return null;
	}

	private static class FilteredTreeModel extends DefaultTreeModel {

		private final JTextField filter;
		private final DefaultMutableTreeNode rootNode;
		private final DefaultMutableTreeNode unfilteredRootNode;
		private final Map<TreeNode, TreeNode> transformation;

		public FilteredTreeModel(DefaultTreeModel model, JTextField filterTextField) {
			super(cloneRootNode(model));

			filter = filterTextField;
			rootNode = (DefaultMutableTreeNode) root;
			unfilteredRootNode = (DefaultMutableTreeNode) model.getRoot();
			transformation = new HashMap<>();

			rebuild();
		}

		private static TreeNode cloneRootNode(DefaultTreeModel model) {
			DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
			return new DefaultMutableTreeNode(rootNode.getUserObject());
		}

		public void rebuild() {
			transformation.clear();
			rootNode.removeAllChildren();

			transformation.put(unfilteredRootNode, rootNode);
			recursiveFilterNodes(rootNode, unfilteredRootNode);

			reload(rootNode);
		}

		private void recursiveFilterNodes(DefaultMutableTreeNode node, DefaultMutableTreeNode unfilteredNode) {
			for (Enumeration<?> e = unfilteredNode.children(); e.hasMoreElements();) {
				DefaultMutableTreeNode uNode = (DefaultMutableTreeNode) e.nextElement();
				Object userObject = uNode.getUserObject();

				String searchable = null;
				if (userObject instanceof StudentClass) {
					searchable = ((StudentClass) userObject).getName();
				} else if (userObject instanceof User) {
					searchable = ((User) userObject).getFullName();
				}
				DefaultMutableTreeNode newbuiltNode = new DefaultMutableTreeNode(uNode.getUserObject());
				recursiveFilterNodes(newbuiltNode, uNode);

				if (searchable != null && searchable.toLowerCase().contains(filter.getText().toLowerCase())) {
					node.add(newbuiltNode);
					transformation.put(uNode, newbuiltNode);
				} else if (newbuiltNode.getChildCount() > 0) {
					node.add(newbuiltNode);
					transformation.put(uNode, newbuiltNode);
				}
			}
		}

		public TreeNode getTransformationOf(TreeNode original) {
			if (original == null) {
				return null;
			} else {
				return transformation.get(original);
			}
		}

		@Override
		public TreeNode getRoot() {
			return rootNode;
		}
	}

	private class NavigationSelectionListener implements TreeSelectionListener {

		private final MainWindow mainWindow;

		public NavigationSelectionListener(MainWindow mainWindow) {
			this.mainWindow = mainWindow;
		}

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			TreePath path = e.getNewLeadSelectionPath();
			if (path != null) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				if (selectedObject != null && selectedObject.equals(node.getUserObject())) return;

				selectedObject = node.getUserObject();

				if (selectedObject instanceof Host) {
					mainWindow.displayHome();
				} else if (selectedObject instanceof StudentClass) {
					mainWindow.displayClass((StudentClass) selectedObject);
				} else if (selectedObject instanceof User) {
					mainWindow.displayUser((User) selectedObject);
				}
			}
		}
	}

	private class ContextMenuProvider extends MouseAdapter {

		private final @NonNull MainWindow mainWindow;

		public ContextMenuProvider(@NonNull MainWindow mainWindow) {
			this.mainWindow = mainWindow;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			selectClickedObject(e.getY());

			if (e.getButton() == MouseEvent.BUTTON3) { // Right click
				DefaultMutableTreeNode clickedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				if (clickedNode == null) return;

				Object clicked = clickedNode.getUserObject();
				openContextMenu(clicked, e.getX(), e.getY());
			}
		}

		private void selectClickedObject(int y) { // JTrees are dumb.
			TreePath path = tree.getClosestPathForLocation(0, y);
			tree.setSelectionPath(path);
		}

		private void openContextMenu(Object clickedObject, int x, int y) {
			JPopupMenu menu = null;
			if (clickedObject instanceof StudentClass) {
				StudentClass studentClass = (StudentClass) clickedObject;
				menu = PopUpMenus.popUpMenuForClass(mainWindow, studentClass);
			} else if (clickedObject instanceof User) {
				User user = (User) clickedObject;
				menu = PopUpMenus.popUpMenuForUser(mainWindow, user);
			}
			if (menu == null) return;

			menu.remove(0);
			menu.remove(0);
			menu.show(tree, x, y);
		}
	}
}
