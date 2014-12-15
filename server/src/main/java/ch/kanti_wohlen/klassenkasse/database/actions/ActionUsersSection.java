package ch.kanti_wohlen.klassenkasse.database.actions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.users.ActionUser;
import ch.kanti_wohlen.klassenkasse.database.ActionsSection;
import ch.kanti_wohlen.klassenkasse.database.ClassesSection;
import ch.kanti_wohlen.klassenkasse.database.Database;
import ch.kanti_wohlen.klassenkasse.database.RolesSection;
import ch.kanti_wohlen.klassenkasse.database.UsersSection;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class ActionUsersSection extends ActionSubsection<ActionUser, User> {

	public static final String SQL_NAME = "\"ActionUsers\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"actionID\" BIGINT NOT NULL,"
			+ "\"userID\" INT NOT NULL,"
			+ "\"classID\" INT NOT NULL,"
			+ "\"roleID\" INT NOT NULL,"
			+ "\"firstName\" VARCHAR(32) NOT NULL,"
			+ "\"lastName\" VARCHAR(32) NOT NULL,"
			+ "\"username\" VARCHAR(80) NOT NULL,"
			+ "PRIMARY KEY (\"actionID\"),"
			+ "CONSTRAINT \"foreignActionUsersID\""
			+ "  FOREIGN KEY (\"actionID\")"
			+ "  REFERENCES " + ActionsSection.SQL_NAME + " (\"actionID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignActionUsersUserID\""
			+ "  FOREIGN KEY (\"userID\")"
			+ "  REFERENCES " + UsersSection.SQL_NAME + " (\"userID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignActionUsersClassID\""
			+ "  FOREIGN KEY (\"classID\")"
			+ "  REFERENCES " + ClassesSection.SQL_NAME + " (\"classID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignActionUsersRoleID\""
			+ "  FOREIGN KEY (\"roleID\")"
			+ "  REFERENCES " + RolesSection.SQL_NAME + " (\"roleID\")"
			+ "  ON DELETE CASCADE)";

	public static final String SQL_SELECT = "SELECT * FROM " + SQL_NAME + " WHERE \"actionID\"=?";

	public ActionUsersSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected @Nullable User loadById(long id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT)) {
			statement.setLong(1, id);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			User user = fromRow(results);

			return user;
		}
	}

	@Override
	protected void store(long actionId, ActionUser action) throws SQLException {
		try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_SELECT)) {
			statement.setLong(1, actionId);
			ResultSet results = statement.executeQuery();

			boolean update = results.next();
			if (!update) {
				results.moveToInsertRow();
			}

			results.updateLong(1, actionId);
			results.updateInt(2, action.getUserId());
			results.updateInt(3, action.getClassId());
			results.updateInt(4, action.getRoleId());
			results.updateString(5, action.getFirstName());
			results.updateString(6, action.getLastName());
			results.updateString(7, action.getUsername());

			if (update) {
				results.updateRow();
			} else {
				results.insertRow();
			}
		}
	}

	@SuppressWarnings("null")
	private User fromRow(ResultSet results) throws SQLException {
		int userId = results.getInt(2);
		int classId = results.getInt(3);
		int roleId = results.getInt(4);
		String firstName = results.getString(5);
		String lastName = results.getString(6);
		String username = results.getString(7);

		return new User(userId, classId, roleId, firstName, lastName, username, MonetaryValue.ZERO);
	}
}
