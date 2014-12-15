package ch.kanti_wohlen.klassenkasse.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;

@NonNullByDefault
public class ClassVariablesSection extends DatabaseSection<Integer, Map<String, String>> {

	public static final String SQL_NAME = "\"PrintingInformation\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"classID\" INT NOT NULL,"
			+ "\"key\" VARCHAR(32) NOT NULL,"
			+ "\"value\" LONG VARCHAR NOT NULL,"
			+ "PRIMARY KEY (\"classID\", \"key\"),"
			+ "CONSTRAINT \"foreignClassVariablesClassID\""
			+ "  FOREIGN KEY (\"classID\")"
			+ "  REFERENCES " + ClassesSection.SQL_NAME + " (\"classID\")"
			+ "  ON DELETE CASCADE)";

	public static final String SQL_SELECT_ALL = "SELECT * FROM " + SQL_NAME;
	public static final String SQL_SELECT_ID = "SELECT \"key\", \"value\" FROM " + SQL_NAME
			+ " WHERE \"classID\"=?";
	public static final String SQL_INSERT = "INSERT INTO " + SQL_NAME
			+ " (\"classID\", \"key\", \"value\") VALUES (?, ?, ?)";
	public static final String SQL_DELETE = "DELETE FROM " + SQL_NAME
			+ " WHERE \"classID\"=?";

	public ClassVariablesSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected Map<Integer, Map<String, String>> loadAll() throws SQLException {
		try (Statement statement = database.openConnection().createStatement()) {
			Map<Integer, Map<String, String>> result = new HashMap<>();
			ResultSet results = statement.executeQuery(SQL_SELECT_ALL);

			while (results.next()) {
				int classId = results.getInt(1);
				String key = results.getString(2);
				String value = results.getString(3);

				Map<String, String> map = result.get(classId);
				if (map == null) {
					map = new HashMap<>();
					result.put(classId, map);
				}
				map.put(key, value);
			}

			return result;
		}
	}

	@Override
	protected @Nullable Map<String, String> loadById(Integer id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_ID)) {
			Map<String, String> result = new HashMap<>();
			statement.setInt(1, id);
			ResultSet results = statement.executeQuery();

			while (results.next()) {
				String key = results.getString(1);
				String value = results.getString(2);

				result.put(key, value);
			}

			return result;
		}
	}

	@Override
	protected void save(Map<String, String> values, UpdateType updateType) throws SQLException {
		throw new UnsupportedOperationException("Use #update(int, Map<String, String>)");
	}

	public void update(int classId, Map<String, String> values) {
		try (Connection con = database.openConnection()) {
			try (PreparedStatement delete = con.prepareStatement(SQL_DELETE)) {
				delete.setInt(1, classId);
				delete.execute();
			}

			con.setAutoCommit(false);
			try (PreparedStatement insert = con.prepareStatement(SQL_INSERT)) {
				for (Entry<String, String> entry : values.entrySet()) {
					insert.setInt(1, classId);
					insert.setString(2, entry.getKey());
					insert.setString(3, entry.getValue());
					insert.addBatch();
				}
				insert.executeBatch();
			}

			con.commit();
			con.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
			// TODO: Handle
		}
	}
}
