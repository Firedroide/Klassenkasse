package ch.kanti_wohlen.klassenkasse.database;

import java.io.File;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.DatabaseIdProvider;
import ch.kanti_wohlen.klassenkasse.framework.id.IdProvider;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@SuppressWarnings("null")
public class Database {

	public static final String ADMIN_PERMISSIONS = "view,create,edit,delete"; // Basically everything
	public static final String TEACHER_PERMISSIONS = "view," // See everything
			+ "create.payment,create.user.class," // Create payments everywhere, create users in own class
			+ "edit.payment,edit.user.class,edit.class.self," // Edit payments everywhere, edit users in own class
			+ "delete.payment,delete.user.class" // Delete payments everywhere, delete users in own class
			+ "edit.history.self"; // Allow undoing actions
	public static final String CASHIER_PERMISSIONS = 
			"view.class.self,view.user.class," // See only the users of the own class
			+ "create.payment.class,create.user.class," // Create payments and users in own class
			+ "edit.payment.class,edit.user.class,edit.class.self," // Edit payments and users in own class
			+ "delete.payment.class,delete.user.class" // Delete payments and users in own class
			+ "view.history.self,edit.history.self"; // Allow undoing actions
	public static final String STUDENT_PERMISSIONS = "view.class.self"; // See himself but nothing else

	private static final Logger LOGGER = Logger.getLogger(Database.class.getSimpleName());

	private final @NonNull EmbeddedConnectionPoolDataSource src;
	private final @NonNull DatabaseIdProvider idProvider;

	private final @NonNull ClassesSection classesSection;
	private final @NonNull ClassVariablesSection classVariablesSection;
	private final @NonNull RolesSection rolesSection;
	private final @NonNull UsersSection usersSection;
	private final @NonNull PaymentsSection paymentsSection;
	private final @NonNull PaymentUsersSection paymentUsersSection;
	private final @NonNull ActionsSection actionsSection;

	public Database() throws ClassNotFoundException, UnknownHostException, SQLException {
		// Connect to database. Create it if it doesn't yet exist
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		src = new EmbeddedConnectionPoolDataSource();
		src.setDatabaseName("database");
		src.setCreateDatabase("create");

		LOGGER.info("Initializing database.");
		classesSection = new ClassesSection(this);
		classVariablesSection = new ClassVariablesSection(this);
		rolesSection = new RolesSection(this);
		usersSection = new UsersSection(this);
		paymentsSection = new PaymentsSection(this);
		paymentUsersSection = new PaymentUsersSection(this, paymentsSection, usersSection);
		actionsSection = new ActionsSection(this);

		idProvider = new DatabaseIdProvider(this);

		createPresets();
		createQuartzTables();
		LOGGER.info("Database loaded.");
	}

	public void shutdown() {
		try {
			src.setShutdownDatabase("shutdown");
			src.getConnection();
		} catch (SQLException e) {
			// Expected SQL Shutdown error, ignore
		}
	}

	public File getDataFolder() {
		return new File("database");
	}

	public @NonNull IdProvider getIdProvider() {
		return idProvider;
	}

	public @NonNull Connection openConnection() throws SQLException {
		return src.getPooledConnection().getConnection();
	}

	public @NonNull PreparedStatement prepareUpdateableStatement(String sql) throws SQLException {
		return openConnection().prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
	}

	public void createTable(Connection con, String tableName, String createSql) throws SQLException {
		ResultSet tables = con.getMetaData().getTables(null, null, tableName.replace("\"", ""), null);
		if (!tables.next()) { // Table doesn't exist
			try (Statement statement = con.createStatement()) {
				statement.execute(createSql);
			}
		}
	}

	public void createTrigger(Connection con, String tableName, String triggerSql) throws SQLException {
		try (PreparedStatement statement = con.prepareStatement("SELECT * FROM SYS.SYSTRIGGERS WHERE TRIGGERNAME=?")) {
			statement.setString(1, tableName.replace("\"", ""));
			ResultSet triggers = statement.executeQuery();

			if (!triggers.next()) { // Trigger doesn't exist
				try (Statement trigger = con.createStatement()) {
					trigger.execute(triggerSql);
				}
			}
		}
	}

	public void compactDatabase() throws SQLException {
		try (Connection con = openConnection()) {
			ResultSet tables = con.getMetaData().getTables(null, "APP", "%", null);

			while (tables.next()) {
				String schemaName = tables.getString(2);
				String tableName = tables.getString(3);

				try (CallableStatement cs = con.prepareCall("CALL SYSCS_UTIL.SYSCS_INPLACE_COMPRESS_TABLE(?, ?, 1, 1, 1)")) {
					cs.setString(1, schemaName);
					cs.setString(2, tableName);
					cs.execute();
				}
			}
		}
	}

	// PREPARATIONS //
	private void createPresets() throws SQLException {
		// Create the class for admins and the SuperUser with ID 0
		StudentClass noClass = classesSection.getById(0);
		if (noClass == null) {
			noClass = new StudentClass(0, "(Keine Klasse)", MonetaryValue.ZERO, MonetaryValue.ZERO);
			classesSection.update(noClass, UpdateType.CREATION);
		}

		// Create the SuperUser role with ID 0
		Map<Integer, Role> roles = rolesSection.getAll();
		Role role = roles.get(0);
		if (role == null) {
			role = new Role(0, "SuperUser", "*");
			rolesSection.update(role, UpdateType.CREATION);

			// Also create other roles (admin, teacher, cashier, student)
			// Still check if the roles already exist.
			if (!roles.containsKey(1)) {
				rolesSection.update(new Role(1, "Administrator", ADMIN_PERMISSIONS), UpdateType.CREATION);
			}
			if (!roles.containsKey(2)) {
				rolesSection.update(new Role(2, "Lehrer", TEACHER_PERMISSIONS), UpdateType.CREATION);
			}
			if (!roles.containsKey(3)) {
				rolesSection.update(new Role(3, "Kassier", CASHIER_PERMISSIONS), UpdateType.CREATION);
			}
			if (!roles.containsKey(4)) {
				rolesSection.update(new Role(4, "Schüler", STUDENT_PERMISSIONS), UpdateType.CREATION);
			}
		}

		// Finally, create the SuperUser with ID 0. Ensure that it remains the
		// way it is, the SuperUser needs to have all permissions.
		User user = new User(0, noClass.getLocalId(), role.getLocalId(), "SuperUser", "SuperUser", "superuser",
				MonetaryValue.ZERO);
		usersSection.update(user, UpdateType.CREATION);
	}

	private void createQuartzTables() throws SQLException {
		String createSql;
		try (Scanner s = new Scanner(Database.class.getResourceAsStream("/tables_derby.sql"), "UTF-8")) {
			createSql = s.useDelimiter("\\A").next();
		}

		Pattern namePattern = Pattern.compile("CREATE TABLE (\\w+)");
		try (Connection con = openConnection()) {
			for (String table : createSql.split(";")) {
				Matcher nameMatcher = namePattern.matcher(table);
				nameMatcher.find();
				String tableName = nameMatcher.group(1);
				createTable(con, tableName, table);
			}
		}
	}

	// SECTIONS
	public ClassesSection classes() {
		return classesSection;
	}

	public ClassVariablesSection classVariables() {
		return classVariablesSection;
	}

	public RolesSection roles() {
		return rolesSection;
	}

	public UsersSection users() {
		return usersSection;
	}

	public PaymentsSection payments() {
		return paymentsSection;
	}

	public PaymentUsersSection paymentUsers() {
		return paymentUsersSection;
	}

	public ActionsSection actions() {
		return actionsSection;
	}
}
