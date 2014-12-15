package ch.kanti_wohlen.klassenkasse.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.ActionSearchQuery;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;

@NonNullByDefault
public final class PermissionsHelper {

	private PermissionsHelper() {}

	public static boolean hasPermission(Host host, String permission) {
		User loggedInUser = host.getLoggedInUser();
		if (loggedInUser == null) {
			return false;
		}

		Role role = loggedInUser.getRole(host); // Not a huge deal since we cache roles
		if (role.hasPermission(permission)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean canView(Host host, @Nullable StudentClass studentClass) {
		if (studentClass == null) return false;

		User loggedInUser = host.getLoggedInUser();
		if (loggedInUser == null) {
			return false;
		} else if (studentClass.getLocalId() == loggedInUser.getStudentClassId()) {
			if (!hasPermission(host, "view.class.self")) {
				return false;
			}
		} else {
			if (!hasPermission(host, "view.class")) {
				return false;
			}
		}

		return true;
	}

	public static boolean canView(Host host, @Nullable User user) {
		if (user == null) return false;
		if (user.getLocalId() == 0) return true;

		User loggedInUser = host.getLoggedInUser();
		if (loggedInUser == null) {
			return false;
		} else if (user.getLocalId() == loggedInUser.getLocalId()) {
			return true;
		} else if (user.getStudentClassId() == loggedInUser.getStudentClassId()) {
			return hasPermission(host, "view.user.class");
		} else {
			return hasPermission(host, "view.user");
		}
	}

	public static boolean canUpdateClass(Host host, @Nullable StudentClass studentClass, UpdateType updateType) {
		if (studentClass == null) return false;
		return canUpdateClass(host, studentClass.getLocalId(), updateType);
	}

	public static boolean canUpdateClass(Host host, int classId, UpdateType updateType) {
		User loggedInUser = host.getLoggedInUser();
		if (loggedInUser == null) {
			return false;
		}

		String suffix;
		if (classId == loggedInUser.getStudentClassId()) {
			suffix = "class.self";
		} else {
			suffix = "class";
		}

		return hasUpdatePermission(host, updateType, suffix);
	}

	public static void checkUpdatePermission(Host host, @Nullable StudentClass studentClass, UpdateType updateType) {
		if (!canUpdateClass(host, studentClass, updateType)) {
			throw new PermissionsException();
		}
	}

	public static boolean canUpdateUser(Host host, @Nullable User user, UpdateType updateType) {
		if (user == null) return false;

		User loggedInUser = host.getLoggedInUser();
		if (loggedInUser == null) {
			return false;
		}

		// Cannot modify users with a higher rank
		if (user.getRoleId() < loggedInUser.getRoleId()) {
			return false;
		}

		String suffix;
		if (user.getLocalId() == loggedInUser.getLocalId()) {
			suffix = "user.class.self";
		} else if (user.getStudentClassId() == loggedInUser.getStudentClassId()) {
			suffix = "user.class";
		} else {
			suffix = "user";
		}

		return hasUpdatePermission(host, updateType, suffix);
	}

	public static void checkUpdatePermission(Host host, @Nullable User user, UpdateType updateType) {
		if (!canUpdateUser(host, user, updateType)) {
			throw new PermissionsException();
		}
	}

	public static boolean canUpdatePayment(Host host, @Nullable Payment payment, Collection<User> paymentUsers, UpdateType updateType) {
		if (payment == null) return false;

		User loggedInUser = host.getLoggedInUser();
		if (loggedInUser == null) {
			return false;
		}

		String suffix;
		Set<Integer> classIds = new HashSet<>();
		for (User user : paymentUsers) {
			if (!canView(host, user)) return false;
			classIds.add(user.getStudentClassId());
		}

		if (classIds.isEmpty()) {
			suffix = "payment.class.empty";
		} else if (classIds.size() == 1 && classIds.iterator().next() == loggedInUser.getStudentClassId()) {
			suffix = "payment.class";
		} else {
			suffix = "payment";
		}

		return hasUpdatePermission(host, updateType, suffix);
	}

	public static void checkUpdatePermission(Host host, @Nullable Payment payment, Collection<User> paymentUsers, UpdateType updateType) {
		if (!canUpdatePayment(host, payment, paymentUsers, updateType)) {
			throw new PermissionsException();
		}
	}

	private static boolean hasUpdatePermission(Host host, UpdateType updateType, String suffix) {
		switch (updateType) {
		case CREATION:
			return hasPermission(host, "create." + suffix);
		case REMOVAL:
			return hasPermission(host, "delete." + suffix);
		case UPDATE:
			return hasPermission(host, "edit." + suffix);
		default:
			throw new IllegalArgumentException("UpdateType");
		}
	}

	public static @Nullable ActionSearchQuery checkQuery(Host host, @Nullable ActionSearchQuery searchQuery) {
		if (searchQuery == null) return null;
		if (searchQuery.getLimit() <= 0 || searchQuery.getOffset() < 0) return null;

		User loggedInUser = host.getLoggedInUser();
		if (loggedInUser == null) return null;

		ActionSearchQuery clone = searchQuery.clone();
		if (clone.getLimit() > 100) { // Hard limit on the amount of results
			clone.setLimit((short) 100);
		}

		if (!hasPermission(host, "view.history")) {
			clone.setCreator(loggedInUser);
		} else if (!hasPermission(host, "view.history.self")) {
			return null;
		}

		return clone;
	}
}
