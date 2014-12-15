package ch.kanti_wohlen.klassenkasse.ui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.util.HostTreeCellRenderer;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.UserComparator;

import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;

public class UserSelector extends JScrollPane {

	private final CheckBoxTree tree;
	private final Map<Object, DefaultMutableTreeNode> userObjectMap;
	private final Map<StudentClass, Collection<User>> userClassMap;

	/**
	 * Create the user selector without loading the data.
	 * Call {@link #loadData(Host)} to populate the tree.
	 */
	public UserSelector() {
		userObjectMap = new HashMap<>();
		userClassMap = new HashMap<>();

		tree = new CheckBoxTree();
		tree.setClickInCheckBoxOnly(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setModel(new DefaultTreeModel(null));
		tree.setCellRenderer(new HostTreeCellRenderer());
		setViewportView(tree);
	}

	/**
	 * Loads all {@linkplain StudentClass StudentClasses} and all {@linkplain User Users} into the tree.
	 * 
	 * @param host
	 *            the {@link Host} to look up the data from
	 */
	public void loadData(Host host) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(host);
		userObjectMap.put(host, root);

		// Classes
		Map<Integer, StudentClass> classes = new HashMap<>(host.getClasses());
		classes.remove(0);
		List<StudentClass> classList = new ArrayList<>(classes.values());
		Collections.sort(classList, new Comparator<StudentClass>() {

			@Override
			public int compare(StudentClass o1, StudentClass o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		// Users
		for (StudentClass studentClass : classList) {
			DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(studentClass);
			userObjectMap.put(studentClass, classNode);

			List<User> users = new ArrayList<>(host.getUsersByClass(studentClass.getLocalId()).values());
			Collections.sort(users, new UserComparator());
			userClassMap.put(studentClass, users);

			for (User user : users) {
				DefaultMutableTreeNode userNode = new DefaultMutableTreeNode(user);
				classNode.add(userNode);

				userObjectMap.put(user, userNode);
			}
			root.add(classNode);
		}

		tree.setModel(new DefaultTreeModel(root));

		// Less complex tree if only 1 class is visible anyways
		if (classList.size() == 1) {
			tree.setRootVisible(false);
			tree.expandRow(0);
		}
	}

	/**
	 * Ensures that a {@link User} is in the tree, adding it to its {@link StudentClass} if necessary.
	 * 
	 * @param host
	 *            the {@link Host} to be used to look up the {@code User}'s {@code StudentClass}
	 * @param user
	 *            the {@code User} which needs to be in the tree
	 * @return {@code true}, if the {@code User} was added to the tree, {@code false} otherwise
	 */
	public boolean ensureUserAdded(@NonNull Host host, User user) {
		if (userObjectMap.containsKey(user)) return false;

		StudentClass studentClass = user.getStudentClass(host);
		Collection<User> users = userClassMap.get(studentClass);
		if (users == null) throw new IllegalStateException("The user did not belong to any visible classes.");

		users.add(user);
		DefaultMutableTreeNode userNode = new DefaultMutableTreeNode(user);
		DefaultMutableTreeNode classNode = userObjectMap.get(studentClass);
		classNode.add(userNode);
		userObjectMap.put(user, userNode);

		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		model.nodeStructureChanged(classNode);
		return true;
	}

	/**
	 * Checks a user object, which can be the {@link Host}, a {@link StudentClass} or a {@link User}.
	 * 
	 * @param userObject
	 *            the object in the tree to be checked
	 * @param checked
	 *            whether the node should be checked or unchecked
	 * @param expand
	 *            whether the node should be expanded after changing the checked state
	 */
	public void checkByUserObject(Object userObject, boolean checked, boolean expand) {
		CheckBoxTreeSelectionModel selectionModel = tree.getCheckBoxTreeSelectionModel();
		DefaultMutableTreeNode node = userObjectMap.get(userObject);
		if (node == null) throw new IllegalArgumentException("User object was not in the tree.");

		TreePath path = new TreePath(node.getPath());

		if (checked) {
			selectionModel.addSelectionPath(path);
		} else {
			selectionModel.removeSelectionPath(path);
		}

		if (expand) {
			if (userObject instanceof User) {
				tree.makeVisible(path);
			} else {
				tree.expandPath(path);
			}
		}
	}

	/**
	 * Returns all the {@linkplain User Users} which were selected in the tree, directly or indirectly.
	 * 
	 * @return the selected {@code Users} in the tree.
	 */
	public @NonNull Collection<User> getSelectedUsers() {
		TreeSelectionModel checkModel = tree.getCheckBoxTreeSelectionModel();
		TreePath[] paths = checkModel.getSelectionPaths();
		Collection<User> users = new ArrayList<User>();

		for (TreePath path : paths) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			if (node == tree.getModel().getRoot()) {
				return getAllUsers();
			}

			Object userObject = node.getUserObject();
			if (userObject instanceof StudentClass) {
				StudentClass studentClass = (StudentClass) userObject;
				Collection<User> classUsers = userClassMap.get(studentClass);

				users.addAll(classUsers);
			} else if (userObject instanceof User) {
				User user = (User) userObject;
				users.add(user);
			}
		}

		return users;
	}

	/**
	 * Returns all the {@linkplain User Users} in the tree.
	 * 
	 * @return a {@link Collection} of all {@code Users}
	 */
	public @NonNull Collection<User> getAllUsers() {
		Collection<Collection<User>> userCollections = userClassMap.values();
		Collection<User> result = new ArrayList<User>();

		for (Collection<User> classCollection : userCollections) {
			result.addAll(classCollection);
		}

		return result;
	}

	/**
	 * Deselects all elements in the tree.
	 */
	public void deselectAll() {
		tree.setSelectionPath(null);
	}

	/**
	 * Gets the underlying {@link CheckBoxTree}.
	 * Use this for adding listeners or customizing the look of the component.
	 * 
	 * @return the underlying {@code CheckBoxTree}
	 */
	public CheckBoxTree getCheckBoxTree() {
		return tree;
	}
}
