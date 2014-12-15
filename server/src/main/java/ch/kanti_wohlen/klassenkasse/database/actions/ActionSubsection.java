package ch.kanti_wohlen.klassenkasse.database.actions;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.database.ActionsSection;
import ch.kanti_wohlen.klassenkasse.database.Database;

//TODO: Handle all SQLExceptions
@NonNullByDefault
public abstract class ActionSubsection<A extends Action, V> {

	protected final Database database;

	private static String createCascadeTrigger(String tableName) {
		return "CREATE TRIGGER " + tableName
			+ "  AFTER DELETE ON " + tableName
			+ "  REFERENCING OLD AS \"Removed\""
			+ "  FOR EACH ROW"
			+ "  DELETE FROM " + ActionsSection.SQL_NAME
			+ "    WHERE \"actionID\" = \"Removed\".\"actionID\"";
	}

	public ActionSubsection(Database database, String tableName, String createSql) throws SQLException {
		this.database = database;

		try (Connection connection = database.openConnection()) {
			database.createTable(connection, tableName, createSql);
			database.createTrigger(connection, tableName, createCascadeTrigger(tableName));
		}
	}

	public @Nullable V getById(long actionId) {
		try {
			return loadById(actionId);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void update(long actionId, A action) {
		try {
			store(actionId, action);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected abstract @Nullable V loadById(long actionId) throws SQLException;

	protected abstract void store(long actionId, A action) throws SQLException;
}
