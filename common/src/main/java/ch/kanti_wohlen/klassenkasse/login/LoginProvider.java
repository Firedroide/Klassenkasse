package ch.kanti_wohlen.klassenkasse.login;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.network.Protocol.NetworkError;

/**
 * Represents the login server the server instance uses to authenticate their users.
 * <p>
 * Also has a method to retrieve all user names from the login server.
 * </p>
 * 
 * @author Roger Baumgartner
 */
@NonNullByDefault
public interface LoginProvider {

	/**
	 * Gets a list of user names from this {@link LoginProvider} which can be used to
	 * {@linkplain #logIn(String, String, boolean) log in}.
	 * 
	 * @return a {@link Collection} containing all user names of this {@code LoginProvider}
	 * @throws LoginServerException
	 *             if the user names could not be retrieved due to a network error.
	 */
	Collection<String> getUserNames() throws LoginServerException;

	/**
	 * Attempts to log in at this {@link LoginProvider}.
	 * <p>
	 * If the login succeeds:
	 * <ul>
	 * <li>A login token will be generated and returned from this method.</li>
	 * <li>No token is generated and null is returned.<br>
	 * The user will still be authenticated, there's just no token for further authentications.</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If the login fails, a {@link LoginServerException} will be thrown.
	 * </p>
	 * 
	 * @param username
	 *            the name of the user to be logged in, never null
	 * @param credentials
	 *            either a password or a login token, never null
	 * @param isToken
	 *            true if the credentials are a login token, false if it's a password (encrypted or not)
	 * @return A {@code String} containing the token or {@code null} if no token was generated.
	 * @throws LoginServerException
	 *             if the login failed.<br>
	 *             The error is {@link NetworkError#INVALID_LOGIN} if the credentials were wrong,
	 *             {@link NetworkError#UNKNOWN_ERROR} otherwise.
	 */
	String logIn(String username, String credentials, boolean isToken) throws LoginServerException;
}
