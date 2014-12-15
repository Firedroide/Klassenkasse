package ch.kanti_wohlen.klassenkasse.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;

//TODO: Handle all SQLExceptions
@NonNullByDefault
public abstract class DatabaseSection<K extends Number, V> {

	protected final static String NON_REMOVED = " WHERE \"isRemoved\"=FALSE";

	protected final Database database;

	public DatabaseSection(Database database, String tableName, String createSql) throws SQLException {
		this.database = database;
		try (Connection con = database.openConnection()) {
			database.createTable(con, tableName, createSql);
		}
	}

	public Map<K, V> getAll() {
		try {
			return loadAll();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new HashMap<>();
	}

	public @Nullable V getById(K id) {
		try {
			return loadById(id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void update(V value, UpdateType updateType) {
		try {
			save(value, updateType);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected abstract Map<K, V> loadAll() throws SQLException;

	protected abstract @Nullable V loadById(K id) throws SQLException;

	protected abstract void save(V value, UpdateType updateType) throws SQLException;
}
