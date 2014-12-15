package ch.kanti_wohlen.klassenkasse.database.actions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.actions.ActionActions;
import ch.kanti_wohlen.klassenkasse.database.ActionsSection;
import ch.kanti_wohlen.klassenkasse.database.Database;

@NonNullByDefault
public class ActionActionsSection extends ActionSubsection<ActionActions, Collection<Long>> {

	public static final String SQL_NAME = "\"ActionActions\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"actionID\" BIGINT NOT NULL,"
			+ "\"appliedID\" BIGINT NOT NULL,"
			+ "PRIMARY KEY (\"actionID\", \"appliedID\"),"
			+ "CONSTRAINT \"foreignActionActionsID\""
			+ "  FOREIGN KEY (\"actionID\")"
			+ "  REFERENCES " + ActionsSection.SQL_NAME + " (\"actionID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignActionActionsAppliedID\""
			+ "  FOREIGN KEY (\"appliedID\")"
			+ "  REFERENCES " + ActionsSection.SQL_NAME + " (\"actionID\")"
			+ "  ON DELETE CASCADE)";

	public static final String SQL_SELECT = "SELECT \"appliedID\" FROM " + SQL_NAME + " WHERE \"actionID\"=?";
	public static final String SQL_INSERT = "INSERT INTO " + SQL_NAME + " (\"actionID\", \"appliedID\") VALUES (?, ?)";

	public ActionActionsSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected @Nullable Collection<Long> loadById(long id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT)) {
			Collection<Long> result = new ArrayList<Long>();
			statement.setLong(1, id);
			ResultSet results = statement.executeQuery();

			while (results.next()) {
				long appliedId = results.getLong(1);
				result.add(appliedId);
			}

			return result;
		}
	}

	@Override
	protected void store(long actionId, ActionActions action) throws SQLException {
		try (Connection con = database.openConnection()) {
			con.setAutoCommit(false);
			try (PreparedStatement insert = con.prepareStatement(SQL_INSERT)) {
				Collection<Long> appliedIds = action.getActionIds();
				for (long appliedId : appliedIds) {
					insert.setLong(1, actionId);
					insert.setLong(2, appliedId);
					insert.addBatch();
				}
				insert.executeBatch();
			}

			con.commit();
			con.setAutoCommit(true);
		}
	}
}
