package ch.kanti_wohlen.klassenkasse.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.zeroturnaround.zip.ZipUtil;

import ch.kanti_wohlen.klassenkasse.database.actions.ActionClassesSection;
import ch.kanti_wohlen.klassenkasse.database.actions.ActionPaymentsSection;
import ch.kanti_wohlen.klassenkasse.database.actions.ActionUsersSection;
import ch.kanti_wohlen.klassenkasse.server.Server;

public class BackupScheduler implements Job {

	private static final Logger LOGGER = Logger.getLogger(BackupScheduler.class.getSimpleName());
	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
			DateFormat.MEDIUM, Locale.forLanguageTag("de-ch"));

	public static final String REMOVED = " WHERE \"isRemoved\"=TRUE";
	public static final String SQL_SELECT_CLASSES = "SELECT \"classID\" FROM " + ClassesSection.SQL_NAME + REMOVED;
	public static final String SQL_SELECT_USERS = "SELECT \"userID\" FROM " + UsersSection.SQL_NAME + REMOVED;
	public static final String SQL_SELECT_PAYMENTS = "SELECT \"paymentID\" FROM " + PaymentsSection.SQL_NAME + REMOVED;

	public static final String SQL_SELECT_CLASS_ACTIONS = "SELECT * FROM " + ActionClassesSection.SQL_NAME
			+ "  INNER JOIN " + ActionsSection.SQL_NAME
			+ "  ON " + ActionsSection.SQL_NAME + ".\"actionID\"=" + ActionClassesSection.SQL_NAME + ".\"actionID\""
			+ "  WHERE " + ActionClassesSection.SQL_NAME + ".\"classID\"=?"
			+ "  AND " + ActionsSection.SQL_NAME + ".\"creationTime\">?";

	public static final String SQL_SELECT_USER_ACTIONS = "SELECT * FROM " + ActionUsersSection.SQL_NAME
			+ "  INNER JOIN " + ActionsSection.SQL_NAME
			+ "  ON " + ActionsSection.SQL_NAME + ".\"actionID\"=" + ActionUsersSection.SQL_NAME + ".\"actionID\""
			+ "  WHERE " + ActionUsersSection.SQL_NAME + ".\"userID\"=?"
			+ "  AND " + ActionsSection.SQL_NAME + ".\"creationTime\">?";

	public static final String SQL_SELECT_PAYMENT_ACTIONS = "SELECT * FROM " + ActionPaymentsSection.SQL_NAME
			+ "  INNER JOIN " + ActionsSection.SQL_NAME
			+ "  ON " + ActionsSection.SQL_NAME + ".\"actionID\"=" + ActionPaymentsSection.SQL_NAME + ".\"actionID\""
			+ "  WHERE " + ActionPaymentsSection.SQL_NAME + ".\"paymentID\"=?"
			+ "  AND " + ActionsSection.SQL_NAME + ".\"creationTime\">?";

	private static Scheduler scheduler;

	public static void start() throws SchedulerException {
		scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();

		if (!scheduler.checkExists(JobKey.jobKey(BackupScheduler.class.getSimpleName()))) {
			JobDetail backup = JobBuilder.newJob(BackupScheduler.class)
					.withIdentity(BackupScheduler.class.getSimpleName())
					.build();

			int backupInterval = Server.INSTANCE.getConfiguration().getInteger("backups.intervalsInDays.backupCreation");
			Trigger trigger = TriggerBuilder.newTrigger().forJob(backup)
					.withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
							.withIntervalInDays(backupInterval))
					.withIdentity("BackupTrigger")
					.startNow()
					.build();

			scheduler.scheduleJob(backup, trigger);
		}
	}

	public static void shutdown() {
		if (scheduler == null) return;

		try {
			scheduler.shutdown(true);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

	public BackupScheduler() {}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LOGGER.info("=== STARTED DATABASE MAINTENANCE JOB ===");

		Database database = Server.INSTANCE.getDatabase();
		File backupsFolder = new File(Server.INSTANCE.getConfiguration().getString("backups.directory"));
		backupsFolder.mkdir();

		zipDatabase(database, backupsFolder);
		removeOldBackups(backupsFolder);

		cleanDatabase(database);

		LOGGER.info("=== COMPLETED DATABASE MAINTENANCE JOB ===");
	}

	private void zipDatabase(Database database, File backupsFolder) {
		File dataFolder = database.getDataFolder();

		try (Statement statement = database.openConnection().createStatement()) {
			LOGGER.info("=== Starting database backup ===");
			statement.execute("CALL SYSCS_UTIL.SYSCS_FREEZE_DATABASE()"); // Lock the database

			String date = DATE_FORMAT.format(new Date()).replaceAll("[.:]", "-").replace(' ', '_');
			File outputFile = new File(backupsFolder, "database-" + date + ".zip");
			ZipUtil.pack(dataFolder, outputFile);

			statement.execute("CALL SYSCS_UTIL.SYSCS_UNFREEZE_DATABASE()"); // Unlock it again
			LOGGER.info("=== Database backup complete ===");
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Error freezing or unfreezing the database!", e);
		}
	}

	private void removeOldBackups(File backupsFolder) {
		LOGGER.info("=== Removing old backups ===");

		Calendar calendar = Calendar.getInstance();
		int period = Server.INSTANCE.getConfiguration().getInteger("backups.intervalsInDays.backupRemoval");
		calendar.add(Calendar.DATE, -period);
		Date comparisonDate = calendar.getTime();
		Pattern datePattern = Pattern.compile("database-(.+)\\.zip");

		for (File file : backupsFolder.listFiles()) {
			String fileName = file.getName();
			Matcher m = datePattern.matcher(fileName);
			if (!m.matches()) {
				LOGGER.info("Skipping file (not matching file name pattern) " + fileName);
				continue;
			}
			String datePart = m.group(1);
			datePart = datePart.replaceFirst("([\\d]+)-([\\d]+)-([\\d]+)_([\\d]+)-([\\d]+)-([\\d]+)", "$1.$2.$3 $4:$5:$6");

			Date fileDate;
			try {
				fileDate = DATE_FORMAT.parse(datePart);
			} catch (ParseException e) {
				LOGGER.warning("Skipping file (could not parse date) " + fileName);
				continue;
			}

			if (fileDate.before(comparisonDate)) {
				LOGGER.info("Removing backup " + fileName);
				file.delete();
			}
		}

		LOGGER.info("=== Done removing old backups ===");
	}

	private void cleanDatabase(Database database) {
		LOGGER.info("=== Removing deleted objects from the database ===");

		Calendar calendar = Calendar.getInstance();
		int period = Server.INSTANCE.getConfiguration().getInteger("backups.intervalsInDays.oldDatabaseObjectRemoval");
		calendar.add(Calendar.DATE, -period);
		long removalDate = calendar.getTimeInMillis();

		try {
			removeStudentClasses(database, removalDate);
			removeUsers(database, removalDate);
			removePayments(database, removalDate);

			database.compactDatabase();
		} catch (SQLException e) {
			LOGGER.severe("=== Could not remove deleted objects from the database ===");
			e.printStackTrace();
			return;
		}

		LOGGER.info("=== Completed removing deleted objects from the database ===");
	}

	private void removeStudentClasses(Database database, long removalDate) throws SQLException {
		int removed = removeUnmodified(database, removalDate, SQL_SELECT_CLASSES, SQL_SELECT_CLASS_ACTIONS);
		LOGGER.info("Removed " + removed + " StudentClasses deleted prior to "
				+ DATE_FORMAT.format(new Date(removalDate)));
	}

	private void removeUsers(Database database, long removalDate) throws SQLException {
		int removed = removeUnmodified(database, removalDate, SQL_SELECT_USERS, SQL_SELECT_USER_ACTIONS);
		LOGGER.info("Removed " + removed + " Users deleted prior to "
				+ DATE_FORMAT.format(new Date(removalDate)));
	}

	private void removePayments(Database database, long removalDate) throws SQLException {
		int removed = removeUnmodified(database, removalDate, SQL_SELECT_PAYMENTS, SQL_SELECT_PAYMENT_ACTIONS);
		LOGGER.info("Removed " + removed + " Payments deleted prior to "
				+ DATE_FORMAT.format(new Date(removalDate)));
	}

	private int removeUnmodified(Database database, long removalDate, String sqlObject, String sqlAction)
			throws SQLException {
		int count = 0;

		try (Connection con = database.openConnection()) {
			try (Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				try (ResultSet result = statement.executeQuery(sqlObject)) {

					while (result.next()) {
						int id = result.getInt(1);
						int row = result.getRow();

						try (PreparedStatement prepared = con.prepareStatement(sqlAction)) {
							prepared.setInt(1, id);
							prepared.setLong(2, removalDate);

							try (ResultSet preparedResult = prepared.executeQuery()) {
								if (!preparedResult.next()) {
									result.absolute(row);
									result.deleteRow();
									++count;
								}
							}
						}
					}
				}
			}
		}

		return count;
	}
}
