package ch.kanti_wohlen.klassenkasse.login;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.server.Server;
import ch.kanti_wohlen.klassenkasse.util.Configuration;
import ch.kanti_wohlen.klassenkasse.util.SubConfiguration;

/**
 * An implementation of the {@link LoginProvider} interface for an {@code Active Directory} domain controller.
 * <p>
 * Uses the UnboundID LDAP SDK to create a connection to the AD-Server. Supports neither logging in with a token nor
 * with encrypted passwords.
 * </p>
 * <p>
 * This class is configured by the section {@code authentication.ldap} in the configuration file.
 * </p>
 * 
 * @author Roger Baumgartner
 */
public class LdapLoginProvider implements LoginProvider {

	private static final Logger LOGGER = Logger.getLogger(LdapLoginProvider.class.getSimpleName());
	private final Configuration config;
	private final Filter objectFilter;

	private Collection<String> userNames;

	/**
	 * Creates a new {@link LdapLoginProvider} and automatically tests the connection.
	 * This could take up to several seconds to complete.
	 * 
	 * @param ldapSection
	 *            the {@link SubConfiguration} section that configures the LDAP connections,
	 *            found at the path {@code authentication.ldap}
	 * @throws LoginServerException
	 *             if no connection could be established or any user settings were faulty
	 */
	public LdapLoginProvider(Configuration ldapSection) throws LoginServerException {
		this.config = ldapSection;

		try {
			objectFilter = Filter.create(config.getString("objectFilter"));
		} catch (LDAPException e) {
			throw new LoginServerException(e);
		}

		testConnection();

		LOGGER.info("Started LDAP authentication at " + ldapSection.getString("host"));
	}

	/**
	 * Tests the connection to the {@code Active Directory} server,
	 * throwing an exception in case the connection can't be established.
	 * 
	 * @throws LoginServerException
	 *             if the connection failed
	 */
	private void testConnection() throws LoginServerException {
		LDAPConnection connection = null;
		try {
			connection = openConnection();
			if (!connection.isConnected()) {
				LOGGER.severe("Could not connect to the AD server!");
				throw new LoginServerException();
			}
		} catch (LDAPException e) {
			throw new LoginServerException(e);
		} finally {
			if (connection != null) connection.close();
		}
	}

	@Override
	public Collection<String> getUsernames() throws LoginServerException {
		// If the usernames are already cached, return them
		final Collection<String> cached = userNames;
		if (cached != null) {
			LOGGER.finer("Returning cached user names.");
			return cached;
		}

		LOGGER.info("Looking up users from LDAP host...");

		String[] baseDNs = config.getString("baseDN").split(";");
		String queryAttribute = config.getBoolean("hasEMails") ? "mail" : "sAMAccountName";

		List<String> userNames = new ArrayList<>();

		LDAPConnection con = null;
		try {
			con = openConnection();

			for (String baseDn : baseDNs) {
				SearchRequest request = new SearchRequest(baseDn, SearchScope.SUB, objectFilter, queryAttribute);
				SearchResult result = con.search(request);

				for (SearchResultEntry entry : result.getSearchEntries()) {
					String username = entry.getAttributeValue(queryAttribute);
					if (username == null || username.isEmpty()) continue;
					userNames.add(username);
				}
			}

			LOGGER.info("Found " + userNames.size() + " matching users in the directory.");
		} catch (LDAPException e) {
			throw new LoginServerException("Could not get user names", e);
		} finally {
			if (con != null) con.close();
		}

		this.userNames = userNames;
		return userNames;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This implementation doesn't support logging in with a token.
	 * 
	 * @throws UnsupportedOperationException
	 *             if {@code isToken} is @{code true}.
	 */
	@Override
	public String logIn(Host host, String username, char[] credentials, boolean isToken) throws LoginServerException {
		if (isToken) throw new UnsupportedOperationException();

		LOGGER.info("Logging in user " + username);

		String[] baseDNs = config.getString("baseDN").split(";");
		boolean hasEMails = config.getBoolean("hasEMails");
		String queryAttribute = hasEMails ? "mail" : "sAMAccountName";
		String password = new String(credentials); // Login via char[] not possible

		LDAPConnection con = null;
		try {
			con = openConnection();

			Filter userFilter = Filter.createEqualityFilter("sAMAccountName", username);
			if (hasEMails) {
				userFilter = Filter.createORFilter(userFilter, Filter.createEqualityFilter("mail", username));
			}
			userFilter = Filter.createANDFilter(userFilter, objectFilter);

			for (String baseDn : baseDNs) {
				SearchRequest request = new SearchRequest(baseDn, SearchScope.SUB, userFilter, queryAttribute);
				request.setDerefPolicy(DereferencePolicy.ALWAYS);
				SearchResult result = con.search(request);

				if (result.getEntryCount() == 0) continue;
				SearchResultEntry entry = result.getSearchEntries().get(0);

				String userDn = entry.getDN();
				if (userDn == null) throw new IllegalStateException("User without DN");

				String accName = entry.getAttributeValue(queryAttribute);
				if (accName == null || accName.isEmpty()) {
					LOGGER.info("User " + username + " did not have " + queryAttribute + " set.");
					throw new LoginServerException();
				}

				LOGGER.info("Found matching AD entry " + userDn + ", attempting to log in");

				BindResult bindResult = con.bind(userDn, password);
				if (bindResult.getResultCode() != ResultCode.SUCCESS) {
					throw new LoginServerException();
				}

				User userByUsername = Server.INSTANCE.getSuperUserHost().getUserByUsername(accName);
				if (userByUsername == null) {
					LOGGER.info("Could log in user " + username + ", but could not get user object");
					throw new LoginServerException();
				}

				host.setLoggedInUser(userByUsername);
				return null;
			}

			LOGGER.info("Could not log in user " + username);
			throw new LoginServerException();
		} catch (LDAPException e) {
			if (e.getResultCode() == ResultCode.INVALID_CREDENTIALS) {
				LOGGER.info("Non-matching password for user " + username);
				throw new LoginServerException();
			} else {
				LOGGER.log(Level.SEVERE, "Could not log in user, an unexpected exception occurred", e);
				throw new LoginServerException(e);
			}
		} finally {
			if (con != null) con.close();
		}
	}

	/**
	 * Opens a new connection to the Active Directory server with the configured settings loaded.
	 * 
	 * @return the opened {@link LDAPConnection}
	 * @throws LDAPException
	 *             if the connection could not be opened
	 */
	private LDAPConnection openConnection() throws LDAPException {
		String hostname = config.getString("host");
		int port = config.getInteger("port");
		String bindDn = config.getString("bindUserDN");
		String bindPassword = config.getString("bindUserPassword");

		return new LDAPConnection(hostname, port, bindDn, bindPassword);
	}
}
