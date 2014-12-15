package ch.kanti_wohlen.klassenkasse.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.KlassenkassenClient;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.RemoteHost;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.User.NamingMode;
import ch.kanti_wohlen.klassenkasse.ui.util.ConditionalFormatting;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;
import ch.kanti_wohlen.klassenkasse.util.FileConfiguration;

public class MainWindow {

	private static final String PREFERENCES_FILENAME = "preferences.yaml";

	private final @NonNull Host host;
	private final @NonNull FileConfiguration preferences;

	private JFrame mainFrame;
	private ActionBar actionBar;
	private NavigationBar navigationBar;
	private JScrollPane contentScrollPane;
	private JSplitPane splitPane;

	private Object selectedObject;

	/**
	 * Create the window.
	 */
	public MainWindow(@NonNull Host host) {
		this.host = host;

		File preferencesFile;
		if (KlassenkassenClient.PER_USER_CONFIG) {
			File homeDirectory = getPerUserFolder();
			homeDirectory.mkdirs();
			preferencesFile = new File(homeDirectory, PREFERENCES_FILENAME);
			System.out.println(preferencesFile.getAbsolutePath());
		} else {
			preferencesFile = new File(PREFERENCES_FILENAME);
		}

		this.preferences = new FileConfiguration(PREFERENCES_FILENAME, preferencesFile, true);
		setDefaultNamingMode();
		ConditionalFormatting.setConditionalFormatting(preferences);

		initialize();
		displayHome();
	}

	private static File getPerUserFolder() {
		String os = System.getProperty("os.name").toUpperCase();
		if (os.contains("WIN")) {
			return new File(System.getenv("APPDATA") + "\\Klassenkasse");
		} else if (os.contains("MAC")) {
			return new File("~/Library/Application Support/Klassenkasse");
		} else { // We'll assume linux based
			return new File("~/.Klassenkasse");
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		mainFrame = new JFrame(); // Pun intended
		mainFrame.setTitle("Klassenkasse");
		mainFrame.setIconImage(IconProvider.APPLICATION.getImage());
		mainFrame.setMinimumSize(new Dimension(640, 480));
		mainFrame.setBounds(200, 150, 940, 600);
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainFrame.addWindowListener(new WindowAdapter() {

			public void windowClosed(WindowEvent e) {
				if (host instanceof RemoteHost) {
					((RemoteHost) host).stop();
				}
				System.exit(0);
			};
		});

		actionBar = new ActionBar(this);
		mainFrame.getContentPane().add(actionBar, BorderLayout.NORTH);

		splitPane = new JSplitPane();
		splitPane.setDividerSize(2);
		mainFrame.getContentPane().add(splitPane, BorderLayout.CENTER);

		navigationBar = new NavigationBar(this);
		navigationBar.setBorder(new EmptyBorder(2, 2, 1, 2));
		navigationBar.setMinimumSize(new Dimension(100, 41));
		splitPane.setLeftComponent(navigationBar);

		contentScrollPane = new JScrollPane();
		contentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		contentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		contentScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
		contentScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		contentScrollPane.setMinimumSize(new Dimension(200, 25));
		splitPane.setRightComponent(contentScrollPane);
		splitPane.setDividerLocation(225);
	}

	private void setDefaultNamingMode() {
		String modeString = preferences.getString("userNamePreference");

		if (modeString.equals("FIRST_NAME_FIRST")) {
			User.setDefaultNamingMode(NamingMode.FIRST_NAME_FIRST);
		} else if (modeString.equals("LAST_NAME_FIRST")) {
			User.setDefaultNamingMode(NamingMode.LAST_NAME_FIRST);
		}
	}

	public JFrame getFrame() {
		return mainFrame;
	}

	public JPanel getContentPanel() {
		return (JPanel) contentScrollPane.getViewport().getView();
	}

	private void setContentPanel(JPanel newPanel) {
		contentScrollPane.setViewportView(newPanel);
	}

	public void displayHome() {
		selectedObject = host;
		setContentPanel(new HomePanel(this));
		navigationBar.selectHome();
		actionBar.update();
	}

	public void displayClass(StudentClass studentClass) {
		selectedObject = studentClass;
		setContentPanel(new ClassPanel(this, studentClass));
		navigationBar.selectClass(studentClass);
		actionBar.update();
	}

	public void displayUser(User user) {
		selectedObject = user;
		setContentPanel(new UserPanel(this, user));
		navigationBar.selectUser(user);
		actionBar.update();
	}

	public void displayHistory() {
		setContentPanel(new HistoryPanel(this));
		navigationBar.deselect();
		actionBar.update();
	}

	public Object getSelectedObject() {
		return selectedObject;
	}

	public @NonNull Host getHost() {
		return host;
	}

	public void updateAll() {
		navigationBar.loadData(host);
		updateSelected();
	}

	public void updateSelected() {
		if (selectedObject instanceof Host) {
			setContentPanel(new HomePanel(this));
		} else if (selectedObject instanceof StudentClass) {
			StudentClass oldClass = (StudentClass) selectedObject;
			StudentClass newClass = host.getClassById(oldClass.getLocalId());
			setContentPanel(new ClassPanel(this, newClass));
		} else if (selectedObject instanceof User) {
			User oldUser = (User) selectedObject;
			User newUser = host.getUserById(oldUser.getLocalId());
			setContentPanel(new UserPanel(this, newUser));
		}
	}

	public FileConfiguration getPreferences() {
		return preferences;
	}
}
