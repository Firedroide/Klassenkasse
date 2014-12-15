package ch.kanti_wohlen.klassenkasse.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class PaymentsSection extends DatabaseSection<Integer, Payment> {

	public static final String SQL_NAME = "\"Payments\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"paymentID\" INT NOT NULL ,"
			+ "\"date\" BIGINT NOT NULL,"
			+ "\"description\" VARCHAR(128) NOT NULL,"
			+ "\"value\" BIGINT NOT NULL,"
			+ "\"rounding\" BIGINT NOT NULL,"
			+ "\"isRemoved\" BOOLEAN NOT NULL DEFAULT FALSE,"
			+ "PRIMARY KEY (\"paymentID\"))";

	public static final String SQL_SELECT_ALL = "SELECT * FROM " + SQL_NAME + NON_REMOVED;
	public static final String SQL_SELECT_ID = "SELECT * FROM " + SQL_NAME + " WHERE \"paymentID\"=?";

	public PaymentsSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected Map<Integer, Payment> loadAll() throws SQLException {
		try (Statement statement = database.openConnection().createStatement()) {
			Map<Integer, Payment> result = new HashMap<>();
			ResultSet results = statement.executeQuery(SQL_SELECT_ALL);

			while (results.next()) {
				Payment payment = fromRow(results);
				result.put(payment.getLocalId(), payment);
			}

			return result;
		}
	}

	@Override
	protected @Nullable Payment loadById(Integer id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT_ID)) {
			statement.setInt(1, id);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			Payment payment = fromRow(results);

			return payment;
		}
	}

	@Override
	protected void save(Payment value, UpdateType updateType) throws SQLException {
		try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_SELECT_ID)) {
			statement.setInt(1, value.getLocalId());
			ResultSet results = statement.executeQuery();

			boolean update = results.next();
			if (!update) {
				results.moveToInsertRow();
			}

			results.updateInt(1, value.getLocalId());
			results.updateLong(2, value.getDate().getTime());
			results.updateString(3, value.getDescription());
			results.updateLong(4, value.getValue().getCentValue());
			results.updateLong(5, value.getRoundingValue().getCentValue());
			if (updateType == UpdateType.CREATION) {
				results.updateBoolean(6, false);
			} else if (updateType == UpdateType.REMOVAL) {
				results.updateBoolean(6, true);
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
	private Payment fromRow(ResultSet results) throws SQLException {
		int id = results.getInt(1);
		Date date = new Date(results.getLong(2));
		String description = results.getString(3);
		MonetaryValue value = new MonetaryValue(results.getLong(4));
		MonetaryValue rounding = new MonetaryValue(results.getLong(5));

		return new Payment(id, date, description, value, rounding);
	}
}
