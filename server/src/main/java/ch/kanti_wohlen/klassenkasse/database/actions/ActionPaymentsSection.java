package ch.kanti_wohlen.klassenkasse.database.actions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.payments.ActionPayment;
import ch.kanti_wohlen.klassenkasse.database.ActionsSection;
import ch.kanti_wohlen.klassenkasse.database.Database;
import ch.kanti_wohlen.klassenkasse.database.PaymentsSection;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class ActionPaymentsSection extends ActionSubsection<ActionPayment, Payment> {

	public static final String SQL_NAME = "\"ActionPayments\"";

	public static final String SQL_CREATE = "CREATE TABLE " + SQL_NAME
			+ "(\"actionID\" BIGINT NOT NULL,"
			+ "\"paymentID\" INT NOT NULL ,"
			+ "\"date\" BIGINT NOT NULL,"
			+ "\"description\" VARCHAR(128) NOT NULL,"
			+ "\"value\" BIGINT NOT NULL,"
			+ "\"rounding\" BIGINT NOT NULL,"
			+ "PRIMARY KEY (\"actionID\"),"
			+ "CONSTRAINT \"foreignActionPaymentsID\""
			+ "  FOREIGN KEY (\"actionID\")"
			+ "  REFERENCES " + ActionsSection.SQL_NAME + " (\"actionID\")"
			+ "  ON DELETE CASCADE,"
			+ "CONSTRAINT \"foreignActionPaymentsPaymentID\""
			+ "  FOREIGN KEY (\"paymentID\")"
			+ "  REFERENCES " + PaymentsSection.SQL_NAME + " (\"paymentID\")"
			+ "  ON DELETE CASCADE)";

	public static final String SQL_SELECT = "SELECT * FROM " + SQL_NAME + " WHERE \"actionID\"=?";

	public ActionPaymentsSection(Database database) throws SQLException {
		super(database, SQL_NAME, SQL_CREATE);
	}

	@Override
	protected @Nullable Payment loadById(long id) throws SQLException {
		try (PreparedStatement statement = database.openConnection().prepareStatement(SQL_SELECT)) {
			statement.setLong(1, id);
			ResultSet results = statement.executeQuery();

			if (!results.next()) return null;
			Payment payment = fromRow(results);

			return payment;
		}
	}

	@Override
	protected void store(long actionId, ActionPayment action) throws SQLException {
		try (PreparedStatement statement = database.prepareUpdateableStatement(SQL_SELECT)) {
			statement.setLong(1, actionId);
			ResultSet results = statement.executeQuery();

			boolean update = results.next();
			if (!update) {
				results.moveToInsertRow();
			}

			results.updateLong(1, actionId);
			results.updateInt(2, action.getPaymentId());
			results.updateLong(3, action.getPaymentDate().getTime());
			results.updateString(4, action.getPaymentDescription());
			results.updateLong(5, action.getPaymentValue().getCentValue());
			results.updateLong(6, action.getPaymentRounding().getCentValue());

			if (update) {
				results.updateRow();
			} else {
				results.insertRow();
			}
		}
	}

	@SuppressWarnings("null")
	private Payment fromRow(ResultSet results) throws SQLException {
		int paymentId = results.getInt(2);
		Date date = new Date(results.getLong(3));
		String description = results.getString(4);
		MonetaryValue value = new MonetaryValue(results.getLong(5));
		MonetaryValue rounding = new MonetaryValue(results.getLong(6));

		return new Payment(paymentId, date, description, value, rounding);
	}
}
