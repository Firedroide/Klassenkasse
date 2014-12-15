package ch.kanti_wohlen.klassenkasse.login;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.server.Server;
import ch.kanti_wohlen.klassenkasse.util.Configuration;

import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.exception.ApplicationAccessDeniedException;
import com.atlassian.crowd.exception.ExpiredCredentialException;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.InvalidAuthorizationTokenException;
import com.atlassian.crowd.exception.InvalidTokenException;
import com.atlassian.crowd.integration.soap.SOAPAttribute;
import com.atlassian.crowd.integration.soap.SOAPPrincipal;
import com.atlassian.crowd.integration.soap.SearchRestriction;
import com.atlassian.crowd.model.authentication.ApplicationAuthenticationContext;
import com.atlassian.crowd.model.authentication.UserAuthenticationContext;
import com.atlassian.crowd.model.user.UserConstants;
import com.atlassian.crowd.search.SearchContext;
import com.atlassian.crowd.service.soap.client.SecurityServerClient;
import com.atlassian.crowd.service.soap.client.SecurityServerClientImpl;
import com.atlassian.crowd.service.soap.client.SoapClientProperties;

/**
 * An implementation of the {@link LoginProvider} interface for Crowd servers.
 * <p>
 * Supports logging in with a login token. Does not support encrypted passwords.
 * </p>
 * <p>
 * This class is configured by the section {@code authentication.crowd} in the configuration file.
 * </p>
 * 
 * @author Roger Baumgartner
 */
public class CrowdLoginProvider implements LoginProvider {

	private static final Logger LOGGER = Logger.getLogger(CrowdLoginProvider.class.getSimpleName());
	private static final String APPLICATION_NAME = "Klassenkasse";

	private final SecurityServerClient client;

	private Collection<String> userNames;

	/**
	 * Creates a new {@link CrowdLoginProvider} and establishes a connection to the crowd server.
	 * 
	 * @throws LoginServerException
	 *             if no connection could be established
	 */
	public CrowdLoginProvider(Configuration crowdSection) throws LoginServerException {
		SoapClientProperties properties = new ServerSoapClientProperties(crowdSection);
		client = new SecurityServerClientImpl(properties);

		try {
			client.authenticate();
		} catch (RemoteException | InvalidAuthenticationException | InvalidAuthorizationTokenException e) {
			throw new LoginServerException("Failed to authenticate the client application", e);
		}

		LOGGER.info("Started Crowd authentication at " + crowdSection.getString("url"));
	}

	@SuppressWarnings("null")
	@Override
	public Collection<String> getUsernames() throws LoginServerException {
		final Collection<String> cached = userNames;
		if (cached != null) {
			return cached;
		}

		final Set<String> users = new HashSet<>();
		SOAPPrincipal[] principals;

		// Get logins from login server
		try {
			LOGGER.info("Getting usernames.");
			principals = client.findAllPrincipals();
		} catch (RemoteException | InvalidAuthenticationException | InvalidAuthorizationTokenException e) {
			throw new LoginServerException("Failed to get user names from the login server", e);
		}

		for (SOAPPrincipal principal : principals) {
			SOAPAttribute attribute = principal.getAttribute(UserConstants.EMAIL);
			if (attribute != null && attribute.getValues().length > 0) {
				users.add(attribute.getValues()[0]);
			}
		}

		userNames = Collections.unmodifiableSet(users);
		return userNames;
	}

	@Override
	public String logIn(Host host, String username, char[] password, boolean isToken) throws LoginServerException {
		String pwd = new String(password); // We can't pass the password as a char[]
		if (isToken) {
			return logInToken(host, username, pwd);
		} else {
			return logInPassword(host, username, pwd);
		}
	}

	/**
	 * Logs in a user with a login token.
	 * 
	 * @param username
	 *            the user name or e-mail address
	 * @param token
	 *            the transmitted token
	 * @return the same token
	 * @throws LoginServerException
	 *             if the login failed
	 */
	@NonNullByDefault
	private String logInToken(Host host, String username, String token) throws LoginServerException {
		try {
			LOGGER.info("Logging in user " + username + " via token.");
			SOAPPrincipal principal = client.findPrincipalByToken(token);

			String name = principal.getName();
			if (name != null && name.equalsIgnoreCase(username)) {
				LOGGER.fine("Found matching name for login " + username);
				return token;
			}

			SOAPAttribute eMail = principal.getAttribute(UserConstants.EMAIL);
			if (eMail != null && eMail.getValues().length > 0) {
				if (eMail.getValues()[0].equalsIgnoreCase(username)) {
					LOGGER.fine("Found matching e-mail for login " + username);

					User loggedInUser = Server.INSTANCE.getSuperUserHost().getUserByUsername(username);
					if (loggedInUser == null) {
						throw new LoginServerException();
					}
					host.setLoggedInUser(loggedInUser);

					return token;
				}
			}

			throw new LoginServerException(); // Non-matching username / e-mail
		} catch (InvalidAuthenticationException | InvalidTokenException e1) {
			LOGGER.log(Level.FINE, "Invalid token for user " + username, e1);
			throw new LoginServerException(); // Invalid token
		} catch (RemoteException | InvalidAuthorizationTokenException e2) {
			LOGGER.log(Level.SEVERE, "Could not log in user " + username + " by token.", e2);
			throw new LoginServerException(e2); // Other error
		}
	}

	/**
	 * Logs in a user with a password.
	 * 
	 * @param username
	 *            the user name or e-mail address
	 * @param password
	 *            the password in plaintext
	 * @return the login token generated by the crowd server
	 * @throws LoginServerException
	 *             if the login failed
	 */
	@NonNullByDefault
	private @Nullable String logInPassword(Host host, String username, String password) throws LoginServerException {
		String user = username;
		if (username.contains("@")) {
			LOGGER.info("Looking up e-mail " + username);
			try {
				SearchRestriction[] search = {new SearchRestriction(SearchContext.PRINCIPAL_EMAIL, username)};
				SOAPPrincipal[] principals = client.searchPrincipals(search);

				if (principals != null && principals.length == 1) {
					user = principals[0].getName(); // Set name for login
				}
			} catch (RemoteException | InvalidAuthenticationException | InvalidAuthorizationTokenException e) {
				LOGGER.log(Level.SEVERE, "Could not look up user for e-mail " + username, e);
				throw new LoginServerException(e); // e-mail, but can't find it.
			}
		}

		UserAuthenticationContext ctx = new UserAuthenticationContext();
		ctx.setName(user);
		ctx.setCredential(PasswordCredential.unencrypted(password));
		ctx.setApplication(APPLICATION_NAME);

		String token = null;
		try {
			LOGGER.info("Authenticating user " + user);
			token = client.authenticatePrincipal(ctx);

			User loggedInUser = Server.INSTANCE.getSuperUserHost().getUserByUsername(username);
			if (loggedInUser == null) {
				throw new LoginServerException();
			}
			host.setLoggedInUser(loggedInUser);
		} catch (InactiveAccountException | ExpiredCredentialException | InvalidAuthenticationException e1) {
			LOGGER.log(Level.FINE, "Invalid credentials for user " + String.valueOf(user), e1);
			throw new LoginServerException();
		} catch (RemoteException | InvalidAuthorizationTokenException | ApplicationAccessDeniedException e2) {
			LOGGER.log(Level.SEVERE, "Could not log in user " + String.valueOf(user), e2);
			throw new LoginServerException(e2);
		}

		return token;
	}

	private class ServerSoapClientProperties implements SoapClientProperties {

		private final Configuration config;

		public ServerSoapClientProperties(Configuration crowdSection) {
			this.config = crowdSection;
		}

		@Override
		public ApplicationAuthenticationContext getApplicationAuthenticationContext() {
			ApplicationAuthenticationContext result = new ApplicationAuthenticationContext();
			result.setName(getApplicationName());
			result.setCredential(new PasswordCredential(getApplicationPassword()));
			return result;
		}

		@Override
		public String getApplicationAuthenticationURL() {
			return null;
		}

		@Override
		public String getApplicationName() {
			return APPLICATION_NAME;
		}

		@Override
		public String getApplicationPassword() {
			return config.getString("password");
		}

		@Override
		public String getBaseURL() {
			String url = config.getString("url");
			return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
		}

		@Override
		public String getCookieTokenKey() {
			return null;
		}

		@Override
		public String getCookieTokenKey(String defaultKey) {
			return null;
		}

		@Override
		public String getHttpMaxConnections() {
			return null;
		}

		@Override
		public String getHttpProxyHost() {
			return null;
		}

		@Override
		public String getHttpProxyPassword() {
			return null;
		}

		@Override
		public String getHttpProxyPort() {
			return null;
		}

		@Override
		public String getHttpProxyUsername() {
			return null;
		}

		@Override
		public String getHttpTimeout() {
			return null;
		}

		@Override
		public String getSSOCookieDomainName() {
			return null;
		}

		@Override
		public String getSessionLastValidation() {
			return "session.lastvalidation";
		}

		@Override
		public String getSessionTokenKey() {
			return "session.tokenkey";
		}

		@Override
		public long getSessionValidationInterval() {
			return 0;
		}

		@Override
		public String getSocketTimeout() {
			return null;
		}

		@Override
		public void updateProperties(Properties properties) {
			// Do nothing
		}

		@Override
		public String getSecurityServerURL() {
			StringBuilder sb = new StringBuilder();
			sb.append(config.getString("url"));
			if (sb.charAt(sb.length() - 1) != '/') sb.append("/");
			sb.append("services/SecurityServer");
			return sb.toString();
		}
	}
}
