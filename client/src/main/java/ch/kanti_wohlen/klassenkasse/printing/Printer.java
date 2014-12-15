package ch.kanti_wohlen.klassenkasse.printing;

import java.awt.image.BufferedImage;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.RemoteHost;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDataRequest.RequestType;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketPrintingInformation;
import ch.kanti_wohlen.klassenkasse.ui.MainWindow;
import ch.kanti_wohlen.klassenkasse.ui.dialog.PrintDialog;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.StudentClassComparator;
import ch.kanti_wohlen.klassenkasse.ui.util.comparator.UserComparator;

@NonNullByDefault
public final class Printer {

	private static final String PRINT_ERROR_TITLE = "Fehler beim Drucken";
	private static final String PRINT_ERROR = "Beim Drucken ist ein Fehler aufgetreten.\n"
			+ "Bitte überprüfen Sie ihre Druckereinstellungen und "
			+ "starten Sie den Druckvorgang erneut.";
	private static final String PRINTER_OFFLINE = "Der Drucker ist momentan offline.\n"
			+ "Bitte starten Sie den Drucker, und versuchen Sie es erneut.";

	private Printer() {}

	public static void printAllClasses(MainWindow mainWindow) {
		Host host = mainWindow.getHost();
		Map<Integer, StudentClass> classMap = new HashMap<>(host.getClasses());
		classMap.remove(0);

		List<StudentClass> classes = new ArrayList<StudentClass>(classMap.values());
		Collections.sort(classes, new StudentClassComparator());
		PacketPrintingInformation printingInfo = getPrintingInformation(host);

		CollectionPrinter printer = new CollectionPrinter();
		for (StudentClass studentClass : classes) {
			printer.add(new StudentClassPrinter(host, studentClass, printingInfo.getHeaderImage()));
		}

		print(printer, "Alle Klassenübersichten");
	}

	public static void printStudentClass(MainWindow mainWindow, StudentClass studentClass) {
		Host host = mainWindow.getHost();
		PacketPrintingInformation printingInfo = getPrintingInformation(host);
		StudentClassPrinter printer = new StudentClassPrinter(host, studentClass, printingInfo.getHeaderImage());

		print(printer, "Übersicht Klasse " + studentClass.getName());
	}

	public static void printAllUsers(MainWindow mainWindow, StudentClass studentClass) {
		Host host = mainWindow.getHost();
		List<User> users = new ArrayList<User>(host.getUsersByClass(studentClass.getLocalId()).values());
		Collections.sort(users, new UserComparator());

		PacketPrintingInformation printingInfo = getPrintingInformation(host);
		Map<String, String> userVariables = host.getPrintingVariablesForClass(studentClass.getLocalId());
		if (userVariables.isEmpty()) {
			PrintDialog dialog = new PrintDialog(mainWindow, studentClass, printingInfo.getFooterNote());
			dialog.setVisible(true);
			userVariables = dialog.getGeneratedVariables();
		}

		if (!mainWindow.getPreferences().getBoolean("printUserFooterNote")) {
			printingInfo.setFooterNote("");
		}

		CollectionPrinter printer = new CollectionPrinter();
		for (User user : users) {
			if (user == null) continue; // Shouldn't happen
			boolean onePage = mainWindow.getPreferences().getBoolean("printOnePagePerUser");
			printer.add(new UserPrinter(host, user, studentClass, printingInfo, userVariables, onePage));
		}

		print(printer, "Alle Kontoauszüge der Klasse " + studentClass.getName());
	}

	public static void printUser(MainWindow mainWindow, User user) {
		Host host = mainWindow.getHost();
		StudentClass studentClass = user.getStudentClass(host);
		PacketPrintingInformation printingInfo = getPrintingInformation(host);

		Map<String, String> userVariables = host.getPrintingVariablesForClass(studentClass.getLocalId());
		if (userVariables.isEmpty()) {
			PrintDialog dialog = new PrintDialog(mainWindow, studentClass, printingInfo.getFooterNote());
			dialog.setVisible(true);
			userVariables = dialog.getGeneratedVariables();
		}

		if (!mainWindow.getPreferences().getBoolean("printUserFooterNote")) {
			printingInfo.setFooterNote("");
		}

		boolean onePage = mainWindow.getPreferences().getBoolean("printOnePagePerUser");
		UserPrinter printer = new UserPrinter(host, user, studentClass, printingInfo, userVariables, onePage);
		print(printer, "Kontoauszug " + user.getFullName());
	}

	private static void print(Printable printable, String jobName) {
		PrinterJob job = PrinterJob.getPrinterJob();

		job.setJobName(jobName);
		job.setPrintable(printable);
		job.setPageable(new SimplePageable(job, printable));

		if (!job.printDialog()) return;

		try {
			job.print();
		} catch (PrinterException e) {
			if (e instanceof PrinterAbortException) {
				return; // User action
			}

			e.printStackTrace();
			if (e.getMessage() == null || !e.getMessage().contains("is not accepting job")) {
				JOptionPane.showMessageDialog(null, PRINT_ERROR, PRINT_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(null, PRINTER_OFFLINE, PRINT_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public static PacketPrintingInformation getPrintingInformation(Host host) {
		if (host instanceof RemoteHost) {
			RemoteHost remote = (RemoteHost) host;
			return remote.request(RequestType.PRINTING_INFORMATION, PacketPrintingInformation.class);
		} else {
			return new PacketPrintingInformation("{Text}", getLogo()); // TODO: Get from client config?
		}
	}

	private static @Nullable BufferedImage getLogo() {
		File logoFile = new File("logo.png");
		if (!logoFile.isFile()) return null;

		try {
			return ImageIO.read(logoFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
