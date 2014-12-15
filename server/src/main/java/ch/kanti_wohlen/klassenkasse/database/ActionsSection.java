package ch.kanti_wohlen.klassenkasse.database;

import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.*;
import ch.kanti_wohlen.klassenkasse.action.actions.*;
import ch.kanti_wohlen.klassenkasse.action.classes.*;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.*;
import ch.kanti_wohlen.klassenkasse.action.payments.*;
import ch.kanti_wohlen.klassenkasse.action.users.*;
import ch.kanti_wohlen.klassenkasse.database.actions.*;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.network.Protocol;

@NonNullByDefault
public class ActionsSection extends DatabaseSection<Long, BaseAction> {

	public static final String SQL_NAME = "\"Actions\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"actionID\" BIGINT NOT NULL ,"
			+ "\"actionType\" SMALLINT NOT NULL,"
			+ "\"creatorID\" INT NOT NULL,"
			+ "\"creationTime\" BIGINT NOT NULL,"
			+ "\"applied\" BOOLEAN NOT NULL,"
			+ "PRIMARY KEY (\"actionID\"),"
			+ "CONSTRAINT \"foreignActionCreatorID\" FOREIGN KEY (\"creatorID\")"
			+ "  REFERENCES " + UsersSection.SQL_NAME + " (\"userID\")"
			+ "  ON DELETE CASCADE)";

	public static final String SQL_SELECT_ALL = "SELECT * FROM " + SQL_NAME;
	public static final String SQL_SELECT_ID = SQL_SELECT_ALL + " WHERE \"actionID\"=?";
	public static final String SQL_SELECT_UNDO = "SELECT \"applied\" FROM " + SQL_NAME + " WHERE \"actionID\"=?";
	public static final String SQL_INSERT = "INSERT INTO " + SQL_NAME
			+ "  (\"actionID\", \"actionType\", \"creatorID\", \"creationTime\", \"applied\")"
			+ "  VALUES (?, ?, ?, ?, ?)";
	public static final String SQL_SET_UNDONE = "UPDATE " + SQL_NAME + " SET \"isUndone\"=? WHERE \"actionID\"=?";

	public static final String SQL_SEARCH = "SELECT " + ActionsSection.SQL_NAME + ".* FROM " + ActionsSection.SQL_NAME
			+ " LEFT JOIN " + ActionClassesSection.SQL_NAME + " ON " + ActionsSection.SQL_NAME + ".\"actionID\" = "
			+ ActionClassesSection.SQL_NAME + ".\"actionID\""
			+ " LEFT JOIN " + ActionUsersSection.SQL_NAME + " ON " + ActionsSection.SQL_NAME + ".\"actionID\" = "
			+ ActionUsersSection.SQL_NAME + ".\"actionID\""
			+ " LEFT JOIN " + ActionPaymentsSection.SQL_NAME + " ON " + ActionsSection.SQL_NAME + ".\"actionID\" = "
			+ ActionPaymentsSection.SQL_NAME + ".\"actionID\""
			+ " LEFT JOIN " + ActionPaymentUsersSection.SQL_NAME + " ON " + ActionsSection.SQL_NAME
			+ ".\"actionID\" = " + ActionPaymentUsersSection.SQL_NAME + ".\"actionID\""
			+ " LEFT JOIN " + ActionActionsSection.SQL_NAME + " ON " + ActionsSection.SQL_NAME + ".\"actionID\" = "
			+ ActionActionsSection.SQL_NAME + ".\"actionID\"";

	private final ActionClassesSection actionClassesSection;
	private final ActionUsersSection actionUsersSection;
	private final ActionPaymentsSection actionPaymentsSection;
	private final ActionPaymentUsersSection actionPaymentUsersSection;
	private final ActionActionsSection actionActionsSection;

	public ActionsSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);

		actionClassesSection = new ActionClassesSection(database);
		actionUsersSection = new ActionUsersSection(database);
		actionPaymentsSection = new ActionPaymentsSection(database);
		actionPaymentUsersSection = new ActionPaymentUsersSection(database);
		actionActionsSection = new ActionActionsSection(database);
	}

	@Override
	protected Map<Long, BaseAction> loadAll() throws SQLException {
		try (Statement statement = database.openConnection().createStatement()) {
			Map<Long, BaseAction> result = new HashMap<>();
			ResultSet results = statement.executeQuery(SQL_SELECT_ALL);

			while (results.next()) {
				BaseAction action = fromRow(results);
				result.put(action.getLocalId(), action);
			}

			return result;
		}
	}

	@Override
	protected @Nullable BaseAction loadById(Long id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_ID)) {
			statement.setLong(1, id);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			BaseAction baseAction = fromRow(results);

			return baseAction;
		}
	}

	@Override
	protected void save(BaseAction value, UpdateType updateType) throws SQLException {
		if (updateType == UpdateType.REMOVAL) {
			throw new UnsupportedOperationException("Cannot remove actions.");
		} else if (updateType == UpdateType.CREATION) {
			try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_INSERT)) {
				statement.setLong(1, value.getLocalId());
				statement.setByte(2, value.getAction().getActionId());
				statement.setInt(3, value.getCreatorId());
				statement.setLong(4, value.getCreationDate().getTime());
				statement.setBoolean(5, true);

				statement.execute();
			}
		} else {
			try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_SELECT_UNDO)) {
				statement.setLong(1, value.getLocalId());
				ResultSet results = statement.executeQuery();

				if (!results.next()) {
					throw new IllegalArgumentException("Action ID " + value.getLocalId() + " was not in the DB!");
				}

				results.updateBoolean(1, value.isApplied());
				results.updateRow();
			}
		}

		updateAction(value.getLocalId(), value.getAction(), updateType);
	}

	public Map<Long, BaseAction> search(ActionSearchQuery searchQuery) {
		String query = buildSearchQuery(searchQuery);

		try (Statement statement = database.openConnection().createStatement()) {
			Map<Long, BaseAction> result = new HashMap<>();
			ResultSet results = statement.executeQuery(query);

			while (results.next()) {
				BaseAction action = fromRow(results);
				result.put(action.getLocalId(), action);
			}

			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			// TODO: Handle
		}

		return new HashMap<>();
	}

	private BaseAction fromRow(ResultSet results) throws SQLException {
		long actionId = results.getLong(1);
		byte actionType = results.getByte(2);
		int creatorId = results.getInt(3);
		Date creationTime = new Date(results.getLong(4));
		boolean applied = results.getBoolean(5);

		Action action = getActionByType(actionId, actionType);
		return new BaseAction(action, actionId, creatorId, creationTime, applied);
	}

	private Action getActionByType(long actionId, byte actionType) {
		Class<? extends Action> actionClass = Protocol.getActionClassById(actionType);
		Action action;

		if (ActionClass.class.isAssignableFrom(actionClass)) {
			StudentClass studentClass = actionClassesSection.getById(actionId);
			if (studentClass == null) {
				throw new IllegalStateException("ActionClass did not have a StudentClass assigned");
			}

			if (ActionClassCreated.class.equals(actionClass)) {
				action = new ActionClassCreated(studentClass);
			} else if (ActionClassRemoved.class.equals(actionClass)) {
				action = new ActionClassRemoved(studentClass);
			} else if (ActionClassUpdated.class.equals(actionClass)) {
				action = new ActionClassUpdated(studentClass);
			} else {
				throw new IllegalStateException("Unknown class extending ActionClass");
			}
		} else if (ActionUser.class.isAssignableFrom(actionClass)) {
			User user = actionUsersSection.getById(actionId);
			if (user == null) {
				throw new IllegalStateException("ActionUser did not have a User assigned");
			}

			if (ActionUserCreated.class.equals(actionClass)) {
				action = new ActionUserCreated(user);
			} else if (ActionUserRemoved.class.equals(actionClass)) {
				action = new ActionUserRemoved(user);
			} else if (ActionUserUpdated.class.equals(actionClass)) {
				action = new ActionUserUpdated(user);
			} else {
				throw new IllegalStateException("Unknown class extending ActionUser");
			}
		} else if (ActionPayment.class.isAssignableFrom(actionClass)) {
			Payment payment = actionPaymentsSection.getById(actionId);
			if (payment == null) {
				throw new IllegalStateException("ActionPayment did not have a Payment assigned");
			}

			if (ActionPaymentCreated.class.equals(actionClass)) {
				action = new ActionPaymentCreated(payment);
			} else if (ActionPaymentRemoved.class.equals(actionClass)) {
				action = new ActionPaymentRemoved(payment);
			} else if (ActionPaymentUpdated.class.equals(actionClass)) {
				action = new ActionPaymentUpdated(payment);
			} else {
				throw new IllegalStateException("Unknown class extending ActionUser");
			}
		} else if (ActionPaymentUsers.class.isAssignableFrom(actionClass)) {
			Entry<Integer, Collection<Integer>> entry = actionPaymentUsersSection.getById(actionId);
			if (entry == null) {
				throw new IllegalStateException("ActionPaymentUsers did not have a Payment assigned");
			}

			int paymentId = entry.getKey();
			Collection<Integer> userIds = entry.getValue();
			if (userIds == null || userIds.isEmpty()) {
				throw new IllegalStateException("ActionPaymentUsers did not have any Users assigned");
			}

			if (ActionPaymentUsersAdded.class.equals(actionClass)) {
				action = new ActionPaymentUsersAdded(paymentId, userIds);
			} else if (ActionPaymentUsersRemoved.class.equals(actionClass)) {
				action = new ActionPaymentUsersRemoved(paymentId, userIds);
			} else {
				throw new IllegalStateException("Unknown class extending ActionPaymentUsers");
			}
		} else if (ActionActions.class.isAssignableFrom(actionClass)) {
			Collection<Long> actionIds = actionActionsSection.getById(actionId);
			if (actionIds == null || actionIds.isEmpty()) {
				throw new IllegalStateException("ActionActions did not have any Actions assigned");
			}

			if (ActionActionsRedone.class.equals(actionClass)) {
				action = new ActionActionsRedone(actionIds);
			} else if (ActionActionsUndone.class.equals(actionClass)) {
				action = new ActionActionsUndone(actionIds);
			} else {
				throw new IllegalStateException("Unknown class extending ActionActions");
			}
		} else {
			throw new IllegalArgumentException("Unknown action class.");
		}

		return action;
	}

	private void updateAction(long actionId, Action action, UpdateType updateType) throws SQLException {
		if (action instanceof ActionClass) {
			actionClassesSection.update(actionId, (ActionClass) action);
		} else if (action instanceof ActionUser) {
			actionUsersSection.update(actionId, (ActionUser) action);
		} else if (action instanceof ActionPayment) {
			actionPaymentsSection.update(actionId, (ActionPayment) action);
		} else if (action instanceof ActionPaymentUsers) {
			if (updateType == UpdateType.CREATION) {
				// ActionPaymentUsers are immutable
				actionPaymentUsersSection.update(actionId, (ActionPaymentUsers) action);
			}
		} else if (action instanceof ActionActions) {
			if (updateType == UpdateType.CREATION) {
				// ActionActions are also immutable
				actionActionsSection.update(actionId, (ActionActions) action);
			}
		} else {
			throw new IllegalArgumentException("Unknown action class.");
		}
	}

	private String buildSearchQuery(ActionSearchQuery query) {
		StringBuilder whereClause = new StringBuilder(" WHERE ");
		boolean first = true;

		Integer creatorId = query.getCreatorId();
		if (creatorId != null) {
			if (!first) whereClause.append(" AND ");
			first = false;
			whereClause.append(ActionsSection.SQL_NAME).append(".\"creatorID\" = ").append(creatorId);
		}

		Class<? extends Action> actionClass = query.getActionType();
		if (actionClass != null && !Action.class.equals(actionClass)) {
			if (!first) whereClause.append(" AND ");
			first = false;

			if (Modifier.isAbstract(actionClass.getModifiers())) {
				byte lower = getAbstractActionClassBaseId(actionClass);
				byte upper = (byte) (lower + 3);
				whereClause.append(ActionsSection.SQL_NAME).append(".\"actionType\" BETWEEN ").append(lower);
				whereClause.append(" AND ").append(upper);
			} else {
				byte actionId = Protocol.getActionId(actionClass);
				whereClause.append(ActionsSection.SQL_NAME).append(".\"actionType\" = ").append(actionId);
			}
		}

		Date before = query.getBefore();
		if (before != null) {
			if (!first) whereClause.append(" AND ");
			first = false;
			whereClause.append(ActionsSection.SQL_NAME).append(".\"creationTime\" <= ").append(before.getTime());
		}

		Date after = query.getAfter();
		if (after != null) {
			if (!first) whereClause.append(" AND ");
			first = false;
			whereClause.append(ActionsSection.SQL_NAME).append(".\"creationTime\" >= ").append(after.getTime());
		}

		Boolean applied = query.getApplied();
		if (applied != null) {
			if (!first) whereClause.append(" AND ");
			first = false;

			String value = String.valueOf(applied.booleanValue()).toUpperCase();
			whereClause.append(ActionsSection.SQL_NAME).append(".\"applied\" = ").append(value);
		}

		Integer classId = query.getClassId();
		if (classId != null) {
			if (!first) whereClause.append(" AND ");
			first = false;
			whereClause.append(ActionClassesSection.SQL_NAME).append(".\"classID\" = ").append(classId);
		}

		Integer userId = query.getUserId();
		if (userId != null) {
			if (!first) whereClause.append(" AND ");
			first = false;
			whereClause.append(ActionUsersSection.SQL_NAME).append(".\"userID\" = ").append(userId);
		}

		Integer paymentId = query.getPaymentId();
		if (paymentId != null) {
			if (!first) whereClause.append(" AND ");
			first = false;
			whereClause.append(ActionPaymentsSection.SQL_NAME).append(".\"creatorID\" = ").append(paymentId);
		}

		if (first) {
			whereClause.setLength(whereClause.length() - "WHERE ".length());
		}

		StringBuilder sql = new StringBuilder(SQL_SEARCH).append(whereClause);
		sql.append(" ORDER BY ").append(ActionsSection.SQL_NAME).append(".\"creationTime\" DESC");
		sql.append(" OFFSET ").append(query.getOffset()).append(" ROWS");
		sql.append(" FETCH NEXT ").append(query.getLimit()).append(" ROWS ONLY");

		@SuppressWarnings("null")
		@NonNull
		String result = sql.toString();
		return result;
	}

	private byte getAbstractActionClassBaseId(Class<? extends Action> actionClass) {
		if (actionClass.equals(ActionClass.class)) {
			return 0;
		} else if (actionClass.equals(ActionUser.class)) {
			return 4;
		} else if (actionClass.equals(ActionPayment.class)) {
			return 8;
		} else if (actionClass.equals(ActionPaymentUsers.class)) {
			return 12;
		} else if (actionClass.equals(ActionActions.class)) {
			return 16;
		} else {
			throw new IllegalArgumentException("Unhandled class extending Action " + actionClass.getSimpleName());
		}
	}
}
