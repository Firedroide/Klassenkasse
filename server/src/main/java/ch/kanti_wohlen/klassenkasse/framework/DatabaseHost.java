package ch.kanti_wohlen.klassenkasse.framework;

import static ch.kanti_wohlen.klassenkasse.util.PermissionsHelper.canView;
import static ch.kanti_wohlen.klassenkasse.util.PermissionsHelper.checkQuery;
import static ch.kanti_wohlen.klassenkasse.util.PermissionsHelper.checkUpdatePermission;
import static ch.kanti_wohlen.klassenkasse.util.PermissionsHelper.hasPermission;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.ActionSearchQuery;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.database.Database;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.framework.id.IdProvider;
import ch.kanti_wohlen.klassenkasse.framework.id.ServerIdMapper;
import ch.kanti_wohlen.klassenkasse.login.LoginProvider;
import ch.kanti_wohlen.klassenkasse.server.Server;
import ch.kanti_wohlen.klassenkasse.util.PaymentHelper;
import ch.kanti_wohlen.klassenkasse.util.PermissionsException;

/**
 * A {@link Host} which is backed by a {@link Database} and performs permissions checks on all operations.
 */
@NonNullByDefault(false)
public class DatabaseHost implements Host {

	private static final Logger LOGGER = Logger.getLogger(DatabaseHost.class.getSimpleName());

	private final @NonNull String name;
	private final @NonNull Database database;
	private final @NonNull IdMapper idMapper;
	private final @NonNull LoginProvider loginProvider;

	private User loggedInUser;

	public DatabaseHost(@NonNull String name, @NonNull Database database, @NonNull LoginProvider loginProvider) {
		this.name = name;
		this.database = database;
		this.loginProvider = loginProvider;

		idMapper = new ServerIdMapper();
		loggedInUser = null;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Gets the {@link Database} this {@link DatabaseHost} is being backed by.
	 * 
	 * @return the {@code Database} being used
	 */
	public Database getDatabase() {
		return database;
	}

	@Override
	public IdProvider getIdProvider() {
		return database.getIdProvider();
	}

	@Override
	public IdMapper getIdMapper() {
		return idMapper;
	}

	@SuppressWarnings("null")
	@Override
	public Map<Integer, StudentClass> getClasses() {
		if (hasPermission(this, "view.class")) {
			return database.classes().getAll();
		} else if (hasPermission(this, "view.class.self")) {
			StudentClass ownClass = loggedInUser.getStudentClass(this);
			return Collections.singletonMap(ownClass.getLocalId(), ownClass);
		} else {
			return Collections.emptyMap();
		}
	}

	@SuppressWarnings("null")
	@Override
	public @Nullable StudentClass getClassById(int classId) {
		StudentClass studentClass = database.classes().getById(classId);

		if (canView(this, studentClass)) {
			return studentClass;
		} else {
			return null;
		}
	}

	@Override
	public void updateClass(StudentClass studentClass, UpdateType updateType) {
		StudentClass currentClass = getClassById(studentClass.getLocalId());
		checkUpdatePermission(this, updateType == UpdateType.CREATION ? studentClass : currentClass, updateType);
		database.classes().update(studentClass, updateType);
	}

	@SuppressWarnings("null")
	@Override
	public Map<String, String> getPrintingVariablesForClass(int classId) {
		StudentClass studentClass = database.classes().getById(classId);

		if (!canView(this, studentClass)) {
			return Collections.emptyMap();
		} else {
			Map<String, String> variables = database.classVariables().getById(classId);
			if (variables != null) {
				return variables;
			} else {
				return Collections.emptyMap();
			}
		}
	}

	@Override
	public void updatePrintingVariablesForClass(int classId, Map<String, String> variables) {
		StudentClass studentClass = database.classes().getById(classId);
		if (studentClass == null) return;

		checkUpdatePermission(this, studentClass, UpdateType.UPDATE);

		database.classVariables().update(classId, variables);
	}

	@SuppressWarnings("null")
	@Override
	public Map<Integer, User> getUsers() {
		if (hasPermission(this, "view.user")) {
			return database.users().getAll();
		} else if (hasPermission(this, "view.user.class")) {
			return database.users().getUsersByClass(loggedInUser.getStudentClassId());
		} else {
			return Collections.singletonMap(loggedInUser.getLocalId(), loggedInUser);
		}
	}

	@SuppressWarnings("null")
	@Override
	public Map<Integer, User> getUsersByClass(int classId) {
		if (!canView(this, database.classes().getById(classId))) {
			return Collections.emptyMap();
		}

		return database.users().getUsersByClass(classId);
	}

	@SuppressWarnings("null")
	@Override
	public Map<Integer, User> getUsersWithPayment(int paymentId) {
		Map<Integer, User> users = database.paymentUsers().getLeft(paymentId);
		for (User user : users.values()) {
			if (!canView(this, user)) {
				return Collections.emptyMap();
			}
		}

		return users;
	}

	@SuppressWarnings("null")
	@Override
	public @Nullable User getUserById(int userId) {
		User user = database.users().getById(userId);

		if (canView(this, user)) {
			return user;
		} else {
			return null;
		}
	}

	@Override
	public @Nullable User getUserByUsername(String username) {
		User user = database.users().getUserByUsername(username);

		if (canView(this, user)) {
			return user;
		} else {
			return null;
		}
	}

	@Override
	public @Nullable User getLoggedInUser() {
		return loggedInUser;
	}

	@Override
	public void setLoggedInUser(User user) {
		if (loggedInUser == null) {
			loggedInUser = user;
		} else {
			throw new IllegalStateException("Already logged in.");
		}
	}

	@Override
	public void updateUser(User user, UpdateType updateType) {
		User currentUser = getUserById(user.getLocalId());
		checkUpdatePermission(this, updateType == UpdateType.CREATION ? user : currentUser, updateType);

		// Cannot set a user to a higher rank than oneself is
		if (user.getRoleId() < loggedInUser.getRoleId()) {
			throw new PermissionsException();
		}

		database.users().update(user, updateType);
	}

	@Override
	public Map<Integer, Payment> getPayments() {
		return database.payments().getAll();
	}

	@SuppressWarnings("null")
	@Override
	public Map<Integer, Payment> getPaymentsByUser(int userId) {
		User user = database.users().getById(userId);

		if (canView(this, user)) {
			return database.paymentUsers().getRight(userId);
		} else {
			return Collections.emptyMap();
		}
	}

	@SuppressWarnings("null")
	@Override
	public @Nullable Payment getPaymentById(int paymentId) {
		return database.payments().getById(paymentId);
	}

	@SuppressWarnings("null")
	@Override
	public void updatePayment(Payment payment, UpdateType updateType) {
		Payment currentPayment = getPaymentById(payment.getLocalId());
		if (updateType == UpdateType.CREATION) {
			checkUpdatePermission(this, payment, Collections.<User> emptyList(), updateType);
		} else {
			Map<Integer, User> paymentUsers = database.paymentUsers().getLeft(payment.getLocalId());
			checkUpdatePermission(this, currentPayment, paymentUsers.values(), updateType);
		}
		database.payments().update(payment, updateType);
	}

	@Override
	public void addUsersToPayment(Payment payment, Collection<User> users) {
		updatePaymentUsers(payment, users, false);
	}

	@Override
	public void removeUsersFromPayment(Payment payment, Collection<User> users) {
		updatePaymentUsers(payment, users, true);
	}

	private void updatePaymentUsers(@NonNull Payment payment, @NonNull Collection<User> users, boolean remove) {
		checkUpdatePermission(this, payment, users, UpdateType.UPDATE);

		// First of all, figure out all the rounding stuff
		PaymentHelper.changePaymentUsersRounding(Server.INSTANCE.getSuperUserHost(), payment, users, remove);

		// Then, write the payment users to the database
		database.paymentUsers().updateLeftToRight(payment, users, remove);

		// And finally, update all balances
		PaymentHelper.changePaymentUsersBalance(Server.INSTANCE.getSuperUserHost(), payment, users, remove);
	}

	@Override
	public Map<Integer, Role> getRoles() {
		return database.roles().getAll();
	}

	@SuppressWarnings("null")
	@Override
	public @Nullable Role getRoleById(int roleId) {
		return database.roles().getById(roleId);
	}

	@SuppressWarnings("null")
	@Override
	public @Nullable BaseAction getActionById(long actionId) {
		return database.actions().getById(actionId);
	}

	@Override
	public Map<Long, BaseAction> searchActions(ActionSearchQuery searchQuery) {
		ActionSearchQuery checkedQuery = checkQuery(this, searchQuery);
		if (checkedQuery == null) return new HashMap<>();

		return database.actions().search(checkedQuery);
	}

	@Override
	public void addActions(BaseAction... actions) {
		for (BaseAction base : actions) {

			// Actually apply action
			try {
				base.apply(this);
			} catch (PermissionsException e) {
				LOGGER.info("Could not apply action of type " + base.getAction().getClass().getSimpleName());
				return;
			}

			// Write the action to the database
			database.actions().update(base, UpdateType.CREATION);
			LOGGER.info("Added action " + base.getAction().getClass().getSimpleName() + " with ID " + base.getLocalId());
		}
	}

	@Override
	public void setActionsUndone(boolean undone, BaseAction... actions) {
		for (BaseAction action : actions) {
			if (action.isUndone() == undone) throw new IllegalStateException("Wrong action applied state.");

			LOGGER.info("Set action " + action.getAction().getClass().getSimpleName() + " with ID "
					+ action.getLocalId() + " undone to " + undone);
			if (undone) {
				action.undo(this);
			} else {
				action.apply(this);
			}

			database.actions().update(action, UpdateType.UPDATE);
		}
	}

	@Override
	public LoginProvider getLoginProvider() {
		return loginProvider;
	}
}
