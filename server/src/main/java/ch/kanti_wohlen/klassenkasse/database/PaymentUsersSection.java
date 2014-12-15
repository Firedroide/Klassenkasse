package ch.kanti_wohlen.klassenkasse.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.User;

@NonNullByDefault
public class PaymentUsersSection extends LinkerSection<Integer, Payment, Integer, User> {

	public static final String SQL_NAME = "\"PaymentUsers\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"paymentID\" INT NOT NULL,"
			+ "\"userID\" INT NOT NULL,"
			+ "PRIMARY KEY (\"paymentID\", \"userID\"),"
			+ "CONSTRAINT \"foreignPaymentUserPaymentID\""
			+ "  FOREIGN KEY (\"paymentID\")"
			+ "  REFERENCES " + PaymentsSection.SQL_NAME + " (\"paymentID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignPaymentUserUserID\""
			+ "  FOREIGN KEY (\"userID\")"
			+ "  REFERENCES " + UsersSection.SQL_NAME + " (\"userID\")"
			+ "  ON DELETE CASCADE)";

	public static final String SQL_INSERT = "INSERT INTO " + SQL_NAME + " (\"paymentID\", \"userID\") VALUES (?, ?)";
	public static final String SQL_REMOVE = "DELETE FROM " + SQL_NAME + " WHERE (\"paymentID\"=? AND \"userID\"=?)";

	public static final String SQL_SELECT_PAYMENTS = "SELECT \"paymentID\" FROM " + SQL_NAME + " WHERE \"userID\"=?";
	public static final String SQL_SELECT_USERS = "SELECT \"userID\" FROM " + SQL_NAME + " WHERE \"paymentID\"=?";

	public PaymentUsersSection(Database database, PaymentsSection payments, UsersSection users) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE, payments, users);
	}

	@Override
	protected Map<Integer, User> loadByLeft(Integer id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_USERS)) {
			Map<Integer, User> result = new HashMap<>();
			statement.setInt(1, id);
			ResultSet results = statement.executeQuery();

			while (results.next()) {
				int userId = results.getInt(1);
				User user = rightSection.getById(userId);
				if (user == null) {
					throw new NullPointerException("Missing user with id " + userId);
				}

				result.put(user.getLocalId(), user);
			}

			return result;
		}
	}

	@Override
	protected Map<Integer, Payment> loadByRight(Integer id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_PAYMENTS)) {
			Map<Integer, Payment> result = new HashMap<>();
			statement.setInt(1, id);
			ResultSet results = statement.executeQuery();

			while (results.next()) {
				int paymentId = results.getInt(1);
				Payment payment = leftSection.getById(paymentId);
				if (payment == null) {
					throw new NullPointerException("Missing payment with id " + payment);
				}

				result.put(payment.getLocalId(), payment);
			}

			return result;
		}
	}

	@Override
	protected void storeLeftToRight(Integer left, Collection<Integer> rights, boolean removed) throws SQLException {
		try (Connection con = database.openConnection()) {
			con.setAutoCommit(false);

			String sql = removed ? SQL_REMOVE : SQL_INSERT;
			try (PreparedStatement statement = con.prepareStatement(sql)) {
				for (Integer right : rights) {
					statement.setInt(1, left);
					statement.setInt(2, right);
					statement.addBatch();
				}
				statement.executeBatch();
				con.commit();
			}

			con.setAutoCommit(true);
		}
	}

	@Override
	protected void storeRightToLeft(Integer right, Collection<Integer> lefts, boolean removed) throws SQLException {
		try (Connection con = database.openConnection()) {
			con.setAutoCommit(false);

			String sql = removed ? SQL_REMOVE : SQL_INSERT;
			try (PreparedStatement statement = con.prepareStatement(sql)) {
				for (Integer left : lefts) {
					statement.setInt(1, left);
					statement.setInt(2, right);
					statement.addBatch();
				}
				statement.executeBatch();
				con.commit();
			}

			con.setAutoCommit(true);
		}
	}
}
