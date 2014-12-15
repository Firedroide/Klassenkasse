package ch.kanti_wohlen.klassenkasse.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.quartz.SchedulerException;

import ch.kanti_wohlen.klassenkasse.database.BackupScheduler;
import ch.kanti_wohlen.klassenkasse.database.Database;
import ch.kanti_wohlen.klassenkasse.framework.DatabaseHost;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.login.CrowdLoginProvider;
import ch.kanti_wohlen.klassenkasse.login.FallbackLoginProvider;
import ch.kanti_wohlen.klassenkasse.login.LdapLoginProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginProvider;
import ch.kanti_wohlen.klassenkasse.login.LoginServerException;
import ch.kanti_wohlen.klassenkasse.network.ServerSocket;
import ch.kanti_wohlen.klassenkasse.util.Configuration;
import ch.kanti_wohlen.klassenkasse.util.FileConfiguration;

@NonNullByDefault
public class Server {

	public static final Server INSTANCE = new Server();

	private final FileConfiguration configuration;
	private final LoginProvider loginProvider;
	private final Database database;
	private final ServerSocket socket;

	private final DatabaseHost superUserHost;

	public Server() {
		try {
			configuration = new FileConfiguration("configuration.yaml", true);
			setUpLogger();

			database = new Database();
			loginProvider = getLoginProvider();

			User superUser = database.users().getById(0);
			if (superUser == null) throw new NullPointerException("SuperUser did not exist");
			superUserHost = newHost("SuperUser");
			superUserHost.setLoggedInUser(superUser);

			@SuppressWarnings({"null"})
			@NonNull
			InetAddress address = InetAddress.getByName(configuration.getString("network.host"));
			int port = configuration.getInteger("network.port");

			socket = new ServerSocket(address, port);

		} catch (Exception e) {
			throw new Error(e); // Retrhow as an error
		}
	}

	public FileConfiguration getConfiguration() {
		return configuration;
	}

	public Database getDatabase() {
		return database;
	}

	public DatabaseHost getSuperUserHost() {
		return superUserHost;
	}

	public DatabaseHost newHost(String name) {
		return new DatabaseHost(name, database, loginProvider);
	}

	public static final void main(String... args) throws SchedulerException {
		if (INSTANCE.configuration.getBoolean("backups.enabled")) {
			BackupScheduler.start();
			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					BackupScheduler.shutdown();
				}
			});
		}

		INSTANCE.socket.run();
	}

	private void setUpLogger() throws IOException {
		LogManager.getLogManager().reset();
		Logger root = Logger.getLogger("");

		ConsoleHandler console = new ConsoleHandler();
		console.setLevel(Level.INFO);
		console.setFormatter(new ConsoleFormatter());
		root.addHandler(console);

		FileHandler regularLog = new FileHandler("server.log", true);
		regularLog.setLevel(Level.INFO);
		root.addHandler(regularLog);

		if (configuration.getBoolean("debug")) {
			FileHandler debugLog = new FileHandler("debug.log");
			debugLog.setLevel(Level.ALL);
			root.addHandler(debugLog);
		}
	}

	private LoginProvider getLoginProvider() {
		// Choose best LoginProvider
		String method = configuration.getString("authentication.method");
		if ("crowd".equalsIgnoreCase(method)) {
			try {
				Configuration crowdSection = configuration.getSubsection("authentication.crowd");
				return new CrowdLoginProvider(crowdSection);
			} catch (LoginServerException e) {
				e.printStackTrace();
			}
		} else if ("ldap".equalsIgnoreCase(method)) {
			try {
				Configuration ldapSection = configuration.getSubsection("authentication.ldap");
				return new LdapLoginProvider(ldapSection);
			} catch (LoginServerException e) {
				e.printStackTrace();
			}
		} else if ("none".equalsIgnoreCase(method)) {
			return new FallbackLoginProvider(configuration.getSubsection("authentication.none"));
		}

		return new FallbackLoginProvider();
	}

	private static class ConsoleFormatter extends Formatter {

		@SuppressWarnings("null")
		private static final DateFormat FORMAT = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.forLanguageTag("de-ch"));

		@Override
		public String format(@Nullable LogRecord record) {
			if (record == null) return "";

			StringBuilder out = new StringBuilder();
			Date date = new Date(record.getMillis());
			out.append(FORMAT.format(date)).append(" ");

			out.append("[").append(record.getLevel().getName().toUpperCase()).append("] ");

			String loggerName = record.getLoggerName();
			if (loggerName != null) {
				out.append("[").append(loggerName).append("] ");
			}

			String message = record.getMessage();
			if (message != null) {
				out.append(message);
			}

			Throwable thrown = record.getThrown();
			if (thrown != null) {
				if (message == null) {
					out.append(" ");
				} else {
					out.append("\n");
				}

				StringWriter writer = new StringWriter();
				thrown.printStackTrace(new PrintWriter(writer));
				out.append(writer.toString());
			}

			out.append("\n");

			String result = out.toString();
			if (result == null) throw new IllegalStateException();
			return result;
		}
	}
}
