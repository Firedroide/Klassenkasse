package ch.kanti_wohlen.klassenkasse.login;

import java.util.Collection;

public interface LoginProvider {

	Collection<String> getEMailAddresses() throws LoginServerException;

	String logIn(String eMailAddress, String password, boolean isToken) throws LoginServerException;
}
