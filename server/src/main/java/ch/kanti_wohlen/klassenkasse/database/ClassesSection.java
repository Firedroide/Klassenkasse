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
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class ClassesSection extends CachedDatabaseSection<Integer, StudentClass> {

	public static final String SQL_NAME = "\"Classes\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"classID\" INT NOT NULL ,"
			+ "\"name\" VARCHAR(32) NOT NULL,"
			+ "\"rounding\" BIGINT NOT NULL,"
			+ "\"balance\" BIGINT NOT NULL,"
			+ "\"isRemoved\" BOOLEAN NOT NULL DEFAULT FALSE,"
			+ "PRIMARY KEY (\"classID\"))";

	public static final String SQL_SELECT_ALL = "SELECT * FROM " + SQL_NAME + NON_REMOVED;
	public static final String SQL_SELECT_ID = "SELECT * FROM " + SQL_NAME + " WHERE \"classID\"=?";

	public ClassesSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected Map<Integer, StudentClass> loadAll() throws SQLException {
		try (Statement statement = database.openConnection().createStatement()) {
			Map<Integer, StudentClass> result = new HashMap<>();
			ResultSet results = statement.executeQuery(SQL_SELECT_ALL);

			while (results.next()) {
				StudentClass studentClass = fromRow(results);
				result.put(studentClass.getLocalId(), studentClass);
			}

			return result;
		}
	}

	@Override
	protected @Nullable StudentClass loadById(Integer id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_ID)) {
			statement.setInt(1, id);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			StudentClass studentClass = fromRow(results);

			return studentClass;
		}
	}

	@Override
	protected void save(StudentClass value, UpdateType updateType) throws SQLException {
		try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_SELECT_ID)) {
			statement.setInt(1, value.getLocalId());
			ResultSet results = statement.executeQuery();

			boolean update = results.next();
			if (!update) {
				results.moveToInsertRow();
			}

			results.updateInt(1, value.getLocalId());
			results.updateString(2, value.getName());
			results.updateLong(3, value.getRoundingValue().getCentValue());
			results.updateLong(4, value.getRoundedBalance().getCentValue());
			if (updateType == UpdateType.CREATION) {
				results.updateBoolean(5, false);
			} else if (updateType == UpdateType.REMOVAL) {
				results.updateBoolean(5, true);
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
	private StudentClass fromRow(ResultSet results) throws SQLException {
		int classId = results.getInt(1);
		String name = results.getString(2);
		MonetaryValue rounding = new MonetaryValue(results.getLong(3));
		MonetaryValue balance = new MonetaryValue(results.getLong(4));

		return new StudentClass(classId, name, rounding, balance);
	}
}
