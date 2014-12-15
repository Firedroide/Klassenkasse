package ch.kanti_wohlen.klassenkasse.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClass;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClassCreated;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsersAdded;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPaymentCreated;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUserCreated;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.ui.dialog.ClassDialog;
import ch.kanti_wohlen.klassenkasse.ui.dialog.UserDialog;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import ch.kanti_wohlen.klassenkasse.util.PaymentHelper;

public final class OldFormatImporter {

	private static final DateFormat FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag("de-ch"));

	private OldFormatImporter() {}

	public static void importFromOldFormat(@NonNull MainWindow mainWindow) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Klasse importieren...");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);

		int result = chooser.showDialog(mainWindow.getFrame(), "Importieren");
		if (result == JFileChooser.APPROVE_OPTION) {
			File chosenDirectory = chooser.getSelectedFile();
			if (chosenDirectory == null) return;

			if (isProgramDirectory(chosenDirectory)) {
				chosenDirectory = new File(chosenDirectory, "Data");
			}

			if (isDataDirectory(chosenDirectory)) {
				importFromOldFormat(chosenDirectory, mainWindow.getHost());
				mainWindow.updateAll();
			} else {
				JOptionPane.showMessageDialog(null, "Der Ordner ist kein gültiges Datenverzeichnis", "Fehler", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private static boolean isProgramDirectory(File file) {
		String[] exes = file.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".exe");
			}
		});

		return exes.length > 0;
	}

	private static boolean isDataDirectory(File file) {
		if (!file.isDirectory()) return false;
		if (!file.getName().equals("Data")) return false;

		String[] dataFiles = file.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".rtxt");
			}
		});

		return dataFiles.length > 0;
	}

	public static void importFromOldFormat(@NonNull File dataFolder, @NonNull Host host) {
		File[] dataFiles = dataFolder.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				String fileName = pathname.getName();
				return fileName.endsWith(".rtxt");
			}
		});

		StudentClass studentClass = new StudentClass(host, "Zu importierende Klasse");
		Map<User, Collection<Payment>> userPayments = new HashMap<>();

		// Import all users and make the user assign them roles and logins
		List<User> users = new ArrayList<>();
		List<String> usernameFilter = new ArrayList<>();
		for (File dataFile : dataFiles) {
			String fileName = dataFile.getName();
			String userName = fileName.substring(0, fileName.indexOf("."));

			int firstSpace = fileName.indexOf(" ");
			String firstName = userName.substring(0, firstSpace);
			String lastName = userName.substring(firstSpace + 1);

			@SuppressWarnings("null")
			User user = confirmUser(host, studentClass, firstName, lastName, usernameFilter);
			if (user == null) {
				// User aborted
				return;
			}

			usernameFilter.add(user.getUsername());
			users.add(user);

			Collection<Payment> payments = importOldFormatFile(host, dataFile);
			userPayments.put(user, payments);
		}

		Map<Payment, Collection<User>> paymentUsers = unifyPayments(userPayments);

		// Choose the name of the class and show the created users
		studentClass.setName("Wählen Sie einen Namen");

		ClassDialog classDialog = new ClassDialog(host);
		classDialog.setData(studentClass);
		classDialog.addNewUsers(users);
		classDialog.setVisible(true);

		Action[] createdActions = classDialog.getCreatedActions();
		if (createdActions.length == 0) return;

		// Change first action so we're actually creating the class
		ActionClass classAction = (ActionClass) createdActions[0];
		StudentClass created = new StudentClass(classAction.getStudentClassId(), classAction.getStudentClassName(),
				MonetaryValue.ZERO, MonetaryValue.ZERO);
		createdActions[0] = new ActionClassCreated(created);

		BaseAction[] userActions = new BaseAction[createdActions.length];

		for (int i = 0; i < createdActions.length; ++i) {
			Action action = createdActions[i];
			if (action == null) continue;

			userActions[i] = new BaseAction(action, host);
		}

		BaseAction[] paymentActons = createPaymentActions(host, paymentUsers);

		host.addActions(userActions);
		host.addActions(paymentActons);
	}

	@NonNullByDefault
	private static @Nullable User confirmUser(Host host, StudentClass studentClass, String firstName, String lastName,
			List<String> usernameFilter) {
		UserDialog dialog = new UserDialog(host);
		dialog.setStudentClass(studentClass, true);
		dialog.setName(firstName, lastName);
		dialog.filterUsernames(usernameFilter);
		dialog.setTitle("Benutzername von " + firstName + " " + lastName + " wählen");
		dialog.setVisible(true);

		Action[] createdActions = dialog.getCreatedActions();
		if (createdActions.length == 0) {
			int result = JOptionPane.showConfirmDialog(null, "Soll das Importieren der Nutzer wirklich abgebrochen werden?",
					"Importieren abbrechen", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				return null;
			} else {
				return confirmUser(host, studentClass, firstName, lastName, usernameFilter);
			}
		}

		ActionUserCreated created = (ActionUserCreated) createdActions[0];
		return new User(created.getUserId(), created.getClassId(), created.getRoleId(),
				created.getFirstName(), created.getLastName(), created.getUsername(), MonetaryValue.ZERO);
	}

	@NonNullByDefault
	private static Collection<Payment> importOldFormatFile(Host host, File dataFile) {
		Collection<Payment> payments = new ArrayList<>();

		// Java / VB default system encodings do not match on Windows
		String os = System.getProperty("os.name");
		Charset cs = os.contains("Windows") ? Charset.forName("CP1252") : Charset.defaultCharset();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), cs))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] properties = line.split(";;");

				Date date = FORMAT.parse(properties[0]);
				String description = properties[1].trim();

				double doubleValue = Double.parseDouble(properties[2]);
				MonetaryValue value = new MonetaryValue(Math.round(doubleValue * 100));

				@SuppressWarnings("null")
				Payment payment = new Payment(host, date, description, value, MonetaryValue.ZERO);
				payments.add(payment);
			}
		} catch (FileNotFoundException e) {
			// Can't happen
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException | NumberFormatException e) {
			e.printStackTrace();
		}

		@SuppressWarnings("null")
		@NonNull Collection<Payment> nonNull = payments; // Eclipse. Stahp.
		return nonNull;
	}

	@NonNullByDefault
	private static BaseAction[] createPaymentActions(Host host, Map<Payment, Collection<User>> paymentMap) {
		List<Action> actions = new ArrayList<>();

		for (Entry<Payment, Collection<User>> entry : paymentMap.entrySet()) {
			Payment payment = entry.getKey();
			Collection<User> users = entry.getValue();
			if (payment == null || users == null || users.isEmpty()) continue;

			roundPayment(host, payment, users);

			actions.add(new ActionPaymentCreated(payment));
			actions.add(new ActionPaymentUsersAdded(payment, users));
		}

		BaseAction[] baseActions = new BaseAction[actions.size()];
		for (int i = 0; i < actions.size(); ++i) {
			Action action = actions.get(i);
			if (action == null) continue;

			baseActions[i] = new BaseAction(action, host);
		}

		return baseActions;
	}

	private static void roundPayment(Host host, Payment payment, Collection<User> users) {
		MonetaryValue unroundedCombined = payment.getValue().multiply(users.size());

		@SuppressWarnings("null")
		MonetaryValue combined = PaymentHelper.getBestRoundingValue(host, unroundedCombined, MonetaryValue.ZERO,
				Collections.<User>emptyList(), users);
		MonetaryValue singleValue = new MonetaryValue(combined.getCentValue() / users.size());
		MonetaryValue rounding = unroundedCombined.subtract(combined);

		payment.setValue(singleValue);
		payment.setRoundingValue(rounding);
	}

	// TODO: Check for optimization
	@NonNullByDefault
	private static Map<Payment, Collection<User>> unifyPayments(Map<User, Collection<Payment>> userPayments) {
		Map<Payment, Collection<User>> paymentUsers = new HashMap<>();

		// Okay, first of all...
		// Turn around the 1-n relation, making it a Map<Payment, Collection<User>>
		for (Entry<User, Collection<Payment>> entry : userPayments.entrySet()) {
			User user = entry.getKey();
			Collection<Payment> payments = entry.getValue();

			// Sweet sweet O(n^2)
			for (Payment payment : payments) {
				Collection<User> users = paymentUsers.get(payment);
				if (users == null) {
					users = new ArrayList<>();
					paymentUsers.put(payment, users);
				}
				users.add(user);
			}
		}

		// Having that out of the way, we still have duplicate Payments in the map
		// So we're gonna have another O(n^2) to resolve this
		List<Payment> original = new ArrayList<>(paymentUsers.keySet());
		List<Payment> reduced = new ArrayList<>();

		outer: for (Payment payment : original) {
			for (Payment existing : reduced) {
				if (existing.getDate().equals(payment.getDate())
						&& existing.getValue().equals(payment.getValue())
						&& existing.getDescription().equals(payment.getDescription())) {
					// Already exists!
					// Do the shrinking on the map
					Collection<User> users = paymentUsers.remove(payment);
					Collection<User> existingUsers = paymentUsers.get(existing);
					existingUsers.addAll(users);

					// Then continue with the next element to be checked for duplicates
					continue outer;
				}
			}

			// Didn't already exist
			reduced.add(payment);
		}

		return paymentUsers;
	}
}
