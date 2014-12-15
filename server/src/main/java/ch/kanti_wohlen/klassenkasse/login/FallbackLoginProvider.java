package ch.kanti_wohlen.klassenkasse.login;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.server.Server;
import ch.kanti_wohlen.klassenkasse.util.Configuration;

/**
 * The fallback {@link LoginProvider} that is used if no other {@code LoginProvider} is available.
 * 
 * <p>
 * In its default mode, {@code FallbackLoginProvider} rejects all login attempts except for the superuser.
 * It can be set to {@code allow-all} mode, which will log in every user without checking the password.
 * {@code Allow-all} mode should only be used for testing!
 * </p>
 */
public class FallbackLoginProvider implements LoginProvider {

	private static final Logger LOGGER = Logger.getLogger(FallbackLoginProvider.class.getSimpleName());

	private final boolean allowAll;
	private final List<String> eMails;

	public FallbackLoginProvider() {
		this.allowAll = false;
		this.eMails = new ArrayList<>();
		LOGGER.warning("Using fallback login provider");
	}

	public FallbackLoginProvider(Configuration fallbackSection) {
		this.allowAll = fallbackSection.getBoolean("allowAllConnections");
		this.eMails = new ArrayList<>();
		LOGGER.warning("Using fallback login provider");

		if (allowAll) {
			LOGGER.warning("Allowing all connections!!");
			loadEMails(fallbackSection);
		}
	}

	private void loadEMails(Configuration fallbackSection) {
		Object list = fallbackSection.get("allowedEMails");
		if (list instanceof List<?>) {
			List<?> eMailList = (List<?>) list;
			for (Object eMail : eMailList) {
				eMails.add(String.valueOf(eMail));
			}
		}
	}

	@SuppressWarnings("null")
	@Override
	public Collection<String> getUsernames() throws LoginServerException {
		return Collections.unmodifiableList(eMails);
	}

	@Override
	public String logIn(Host host, String username, char[] password, boolean isToken) throws LoginServerException {
		LOGGER.info("Logging in user " + username);

		if (allowAll) {
			User user = Server.INSTANCE.getSuperUserHost().getUserByUsername(username);
			if (user == null) {
				throw new LoginServerException();
			}
			host.setLoggedInUser(user);

			return null;
		} else {
			throw new LoginServerException(new UnsupportedOperationException());
		}
	}
}
