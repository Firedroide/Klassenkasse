package ch.kanti_wohlen.klassenkasse.database.actions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.classes.ActionClass;
import ch.kanti_wohlen.klassenkasse.database.ActionsSection;
import ch.kanti_wohlen.klassenkasse.database.ClassesSection;
import ch.kanti_wohlen.klassenkasse.database.Database;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class ActionClassesSection extends ActionSubsection<ActionClass, StudentClass> {

	public static final String SQL_NAME = "\"ActionClasses\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"actionID\" BIGINT NOT NULL,"
			+ "\"classID\" INT NOT NULL,"
			+ "\"name\" VARCHAR(32) NOT NULL,"
			+ "PRIMARY KEY (\"actionID\"),"
			+ "CONSTRAINT \"foreignActionClassesID\""
			+ "  FOREIGN KEY (\"actionID\")"
			+ "  REFERENCES " + ActionsSection.SQL_NAME + " (\"actionID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignActionClassesClassID\""
			+ "  FOREIGN KEY (\"classID\")"
			+ "  REFERENCES " + ClassesSection.SQL_NAME + " (\"classID\")"
			+ "  ON DELETE CASCADE)";

	public static final String SQL_SELECT = "SELECT * FROM " + SQL_NAME + " WHERE \"actionID\"=?";

	public ActionClassesSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected @Nullable StudentClass loadById(long id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT)) {
			statement.setLong(1, id);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			StudentClass studentClass = fromRow(results);

			return studentClass;
		}
	}

	@Override
	protected void store(long actionId, ActionClass action) throws SQLException {
		try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_SELECT)) {
			statement.setLong(1, actionId);
			ResultSet results = statement.executeQuery();

			boolean update = results.next();
			if (!update) {
				results.moveToInsertRow();
			}

			results.updateLong(1, actionId);
			results.updateInt(2, action.getStudentClassId());
			results.updateString(3, action.getStudentClassName());

			if (update) {
				results.updateRow();
			} else {
				results.insertRow();
			}
		}
	}

	@SuppressWarnings("null")
	private StudentClass fromRow(ResultSet results) throws SQLException {
		int classId = results.getInt(2);
		String name = results.getString(3);

		return new StudentClass(classId, name, MonetaryValue.ZERO, MonetaryValue.ZERO);
	}
}
