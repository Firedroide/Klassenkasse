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
import ch.kanti_wohlen.klassenkasse.framework.Role;

@NonNullByDefault
public class RolesSection extends CachedDatabaseSection<Integer, Role> {

	public static final String SQL_NAME = "\"Roles\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"roleID\" INT NOT NULL ,"
			+ "\"name\" VARCHAR(32) NOT NULL,"
			+ "\"permissions\" LONG VARCHAR NOT NULL,"
			+ "PRIMARY KEY (\"roleID\"),"
			+ "CONSTRAINT \"uniqueRoleName\""
			+ "  UNIQUE (\"name\"))";

	public static final String SQL_SELECT_ALL = "SELECT * FROM " + SQL_NAME;
	public static final String SQL_SELECT_ID = "SELECT * FROM " + SQL_NAME + " WHERE \"roleID\"=?";

	public RolesSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected Map<Integer, Role> loadAll() throws SQLException {
		try (Statement statement = database.openConnection().createStatement()) {
			Map<Integer, Role> result = new HashMap<>();
			ResultSet results = statement.executeQuery(SQL_SELECT_ALL);

			while (results.next()) {
				Role role = fromRow(results);
				result.put(role.getLocalId(), role);
			}
			return result;
		}
	}

	@Override
	protected @Nullable Role loadById(Integer id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_ID)) {
			statement.setInt(1, id);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			Role role = fromRow(results);

			return role;
		}
	}

	@Override
	protected void save(Role value, UpdateType updateType) throws SQLException {
		try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_SELECT_ID)) {
			statement.setInt(1, value.getLocalId());
			ResultSet results = statement.executeQuery();

			boolean update = results.next();

			if (updateType == UpdateType.REMOVAL) {
				if (update) {
					results.deleteRow();
				} else {
					return; // Not here, nothing to remove
				}
			} else {
				if (!update) {
					results.moveToInsertRow();
				}

				results.updateInt(1, value.getLocalId());
				results.updateString(2, value.getName());
				results.updateString(3, value.getPermissionsString());

				if (update) {
					results.updateRow();
				} else {
					results.insertRow();
				}
			}
		}
	}

	@SuppressWarnings("null")
	private Role fromRow(ResultSet results) throws SQLException {
		int id = results.getInt(1);
		String name = results.getString(2);
		String permissions = results.getString(3);

		Role role = new Role(id, name, permissions);
		return role;
	}
}
