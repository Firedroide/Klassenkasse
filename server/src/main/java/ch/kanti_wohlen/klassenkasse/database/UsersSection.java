package ch.kanti_wohlen.klassenkasse.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class UsersSection extends CachedDatabaseSection<Integer, User> {

	public static final String SQL_NAME = "\"Users\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"userID\" INT NOT NULL,"
			+ "\"classID\" INT NOT NULL,"
			+ "\"roleID\" INT NOT NULL,"
			+ "\"firstName\" VARCHAR(32) NOT NULL,"
			+ "\"lastName\" VARCHAR(32) NOT NULL,"
			+ "\"username\" VARCHAR(80) NOT NULL,"
			+ "\"balance\" BIGINT NOT NULL,"
			+ "\"isRemoved\" BOOLEAN NOT NULL DEFAULT FALSE,"
			+ "PRIMARY KEY (\"userID\"),"
			+ "CONSTRAINT \"foreignUserClassID\""
			+ "  FOREIGN KEY (\"classID\")"
			+ "  REFERENCES " + ClassesSection.SQL_NAME + " (\"classID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignUserRoleID\""
			+ "  FOREIGN KEY (\"roleID\")"
			+ "  REFERENCES " + RolesSection.SQL_NAME + " (\"roleID\")"
			+ "  ON DELETE RESTRICT,"
			+ "CONSTRAINT \"uniqueUsername\""
			+ "  UNIQUE (\"username\"))";

	public static final String SQL_SELECT_ALL = "SELECT * FROM " + SQL_NAME + NON_REMOVED;
	public static final String SQL_SELECT_ID = "SELECT * FROM " + SQL_NAME + " WHERE \"userID\"=?";
	public static final String SQL_SELECT_USERNAME = "SELECT * FROM " + SQL_NAME + NON_REMOVED + " AND \"username\"=?";
	public static final String SQL_SELECT_CLASS = "SELECT * FROM " + SQL_NAME + NON_REMOVED + " AND \"classID\"=?";

	public static final String SQL_INSERT_CHECK = "SELECT * FROM " + SQL_NAME + " WHERE \"isRemoved\"=TRUE AND \"username\"=?";

	public UsersSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected Map<Integer, User> loadAll() throws SQLException {
		try (Statement statement = database.openConnection().createStatement()) {
			Map<Integer, User> result = new HashMap<>();
			ResultSet results = statement.executeQuery(SQL_SELECT_ALL);

			while (results.next()) {
				User user = fromRow(results);
				result.put(user.getLocalId(), user);
			}

			return result;
		}
	}

	public Map<Integer, User> getUsersByClass(Integer classId) {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_CLASS)) {
			Map<Integer, User> result = new HashMap<>();
			statement.setInt(1, classId);
			ResultSet results = statement.executeQuery();

			while (results.next()) {
				User user = fromRow(results);
				result.put(user.getLocalId(), user);
			}

			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return new HashMap<>();
		}
	}

	@Override
	protected @Nullable User loadById(Integer id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_ID)) {
			statement.setInt(1, id);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			User user = fromRow(results);

			return user;
		}
	}

	public @Nullable User getUserByUsername(String username) {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_USERNAME)) {
			statement.setString(1, username);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			User user = fromRow(results);

			return user;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void save(User value, UpdateType updateType) throws SQLException {
		if (updateType == UpdateType.CREATION) {
			// TODO: Temp solution, FIXME
			try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_INSERT_CHECK)) {
				statement.setString(1, value.getUsername());
				ResultSet results = statement.executeQuery();

				// If a user with this username already existed, create him again with the same ID
				// This restores his values and keeps the username unique
				if (results.next()) {
					User oldUser = fromRow(results);
					value = new User(oldUser.getLocalId(), value.getStudentClassId(), value.getRoleId(),
							value.getFirstName(), value.getLastName(), value.getUsername(), oldUser.getBalance());
				}
			}
		}

		try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_SELECT_ID)) {
			statement.setInt(1, value.getLocalId());
			ResultSet results = statement.executeQuery();

			boolean update = results.next();
			if (!update) {
				results.moveToInsertRow();
			}

			results.updateInt(1, value.getLocalId());
			results.updateInt(2, value.getStudentClassId());
			results.updateInt(3, value.getRoleId());
			results.updateString(4, value.getFirstName());
			results.updateString(5, value.getLastName());
			results.updateString(6, value.getUsername());
			results.updateLong(7, value.getBalance().getCentValue());

			if (updateType == UpdateType.CREATION) {
				results.updateBoolean(8, false);
			} else if (updateType == UpdateType.REMOVAL) {
				results.updateBoolean(8, true);
			} else if (!update) {
				throw new IllegalStateException("Cannot update a value that doesn't exist");
			}

			if (update) {
				results.updateRow();
			} else {
				results.insertRow();
			}
		}
	}

	@SuppressWarnings("null")
	private User fromRow(ResultSet results) throws SQLException {
		int userId = results.getInt(1);
		int classId = results.getInt(2);
		int roleId = results.getInt(3);
		String firstName = results.getString(4);
		String lastName = results.getString(5);
		String username = results.getString(6);
		MonetaryValue balance = new MonetaryValue(results.getLong(7));

		return new User(userId, classId, roleId, firstName, lastName, username, balance);
	}
}
