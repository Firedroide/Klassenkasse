package ch.kanti_wohlen.klassenkasse;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.ui.LoginWindow;
import ch.kanti_wohlen.klassenkasse.ui.MainWindow;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;

import com.jgoodies.validation.view.ValidationResultViewFactory;

public class KlassenkassenClient {

	public static boolean PER_USER_CONFIG = false;

	private static final Logger LOGGER = Logger.getLogger(KlassenkassenClient.class.getSimpleName());

	public KlassenkassenClient() throws UnknownHostException {
		optimizeLaunch();
		setLookAndFeel();

		LOGGER.info("Starting LoginWindow");
		LoginWindow dialog = new LoginWindow();
		dialog.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				Host host = ((LoginWindow) e.getWindow()).getSelectedHost();

				if (host != null) {
					LOGGER.info("Starting MainWindow");
					MainWindow mainWindow = new MainWindow(host);
					mainWindow.getFrame().setVisible(true);
				} else {
					System.exit(0);
				}
			}
		});
		dialog.setVisible(true);
	}

	public static final void main(String... args) {
		if (args != null && args.length > 0) {
			if ("-u".equalsIgnoreCase(args[0])) {
				PER_USER_CONFIG = true;
			}
		}

		try {
			new KlassenkassenClient();
		} catch (UnknownHostException e1) {
			LOGGER.log(Level.SEVERE, "CRASHED!", e1);
			return;
		}
	}

	/**
	 * This small method optimizes the launch time of the client.
	 * It reduces the time needed to instantiate the {@code JFrame} from
	 * about 4 seconds down to 50 milliseconds.
	 * <p>
	 * How?
	 * </p>
	 * <p>
	 * Short: Black magic.
	 * </p>
	 * <p>
	 * Long: The improvements are made by first of all disabling the {@code Direct3D} rendering pipeline and secondly by
	 * creating a window and therefore setting up the pipeline before setting the {@code Nimbus Look And Feel}. For a
	 * more in-depth explanation, resort to the shorter explanation above.
	 * </p>
	 */
	private static void optimizeLaunch() {
		System.setProperty("sun.java2d.d3d", "false");

		new Window(null).dispose();
	}

	/**
	 * Sets the Look And Feel to Nimbus, changes the {@code TristateCheckBox} icon
	 * and changes the icons used by {@code JGoodies Validation}.
	 */
	private static void setLookAndFeel() {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}

			// Make popup menus look a bit better
			UIManager.put("MenuItem.background", Color.WHITE);
			UIManager.put("Menu.background", Color.WHITE);
			UIManager.put("MenuItem.foreground", Color.WHITE);
			UIManager.put("Menu.foreground", Color.WHITE);

			// Add mixed icon for the JIDE TristateCheckBox
			Icon icon = new ImageIcon(KlassenkassenClient.class.getResource("/icons/checkbox_mixed.png"));
			UIManager.put("TristateCheckBox.icon", icon);

			// Set the validation icons
			Class<?>[] subClasses = ValidationResultViewFactory.class.getDeclaredClasses();
			for (Class<?> subClass : subClasses) {
				if (!subClass.getSimpleName().equals("Icons")) continue;

				changeField(subClass, "ERROR_ICON", IconProvider.ERROR);
				changeField(subClass, "WARNING_ICON", IconProvider.WARNING);
				changeField(subClass, "INFO_ICON", IconProvider.INFO);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void changeField(Class<?> parent, String fieldName, Object value) {
		try {
			Field field = parent.getDeclaredField(fieldName);
			field.setAccessible(true);

			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

			field.set(null, value);
			modifiers.setInt(field, field.getModifiers() | Modifier.FINAL);
		} catch (Exception e) {
			LOGGER.warning("Could not set icon field " + fieldName);
			e.printStackTrace();
		}
	}
}
