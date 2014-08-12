package ch.kanti_wohlen.klassenkasse.login;

import ch.kanti_wohlen.klassenkasse.framework.User;

public interface UserLogin {

	boolean isLoggedIn();

	User getLoggedInUser();
}
