package ch.kanti_wohlen.klassenkasse.database.actions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsers;
import ch.kanti_wohlen.klassenkasse.database.ActionsSection;
import ch.kanti_wohlen.klassenkasse.database.Database;
import ch.kanti_wohlen.klassenkasse.database.PaymentsSection;
import ch.kanti_wohlen.klassenkasse.database.UsersSection;

@NonNullByDefault
public class ActionPaymentUsersSection extends
		ActionSubsection<ActionPaymentUsers, Entry<Integer, Collection<Integer>>> {

	public static final String SQL_NAME = "\"ActionPaymentUsers\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"actionID\" BIGINT NOT NULL,"
			+ "\"paymentID\" INT NOT NULL,"
			+ "\"userID\" INT NOT NULL,"
			+ "PRIMARY KEY (\"actionID\", \"userID\"),"
			+ "CONSTRAINT \"foreignActionPaymentUsersID\""
			+ "  FOREIGN KEY (\"actionID\")"
			+ "  REFERENCES " + ActionsSection.SQL_NAME + " (\"actionID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignPaymentUsersPaymentID\""
			+ "  FOREIGN KEY (\"paymentID\")"
			+ "  REFERENCES " + PaymentsSection.SQL_NAME + " (\"paymentID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignPaymentUsersUserID\""
			+ "  FOREIGN KEY (\"userID\")"
			+ "  REFERENCES " + UsersSection.SQL_NAME + " (\"userID\")"
			+ "  ON DELETE CASCADE)";

	public static final String SQL_SELECT_ID = "SELECT \"paymentID\", \"userID\" FROM " + SQL_NAME
			+ " WHERE \"actionID\"=?";
	public static final String SQL_INSERT = "INSERT INTO " + SQL_NAME
			+ " (\"actionID\", \"paymentID\", \"userID\") VALUES (?, ?, ?)";

	public ActionPaymentUsersSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected @Nullable Entry<Integer, Collection<Integer>> loadById(long id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_ID)) {
			statement.setLong(1, id);
			ResultSet results = statement.executeQuery();

			Integer paymentId = null;
			Collection<Integer> userIds = new ArrayList<>();
			while (results.next()) {
				if (paymentId == null) {
					paymentId = results.getInt(1);
				}
				userIds.add(results.getInt(2));
			}

			if (paymentId == null) {
				return null;
			} else {
				return new SimpleEntry<>(paymentId, userIds);
			}
		}
	}

	@Override
	protected void store(long actionId, ActionPaymentUsers action) throws SQLException {
		try (Connection con = database.openConnection()) {
			con.setAutoCommit(false);
			try (PreparedStatement insert = con.prepareStatement(SQL_INSERT)) {
				int paymentId = action.getPaymentId();
				Collection<Integer> userIds = action.getUserIds();
				for (int userId : userIds) {
					insert.setLong(1, actionId);
					insert.setInt(2, paymentId);
					insert.setInt(3, userId);
					insert.addBatch();
				}
				insert.executeBatch();
			}

			con.commit();
			con.setAutoCommit(true);
		}
	}
}
