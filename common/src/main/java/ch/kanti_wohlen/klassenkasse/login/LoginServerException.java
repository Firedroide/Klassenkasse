package ch.kanti_wohlen.klassenkasse.login;

public class LoginServerException extends Exception {

	public LoginServerException() {}

	public LoginServerException(String message) {
		super(message);
	}

	public LoginServerException(Throwable cause) {
		super(cause);
	}

	public LoginServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public LoginServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
