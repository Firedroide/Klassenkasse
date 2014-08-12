package ch.kanti_wohlen.klassenkasse.action;

public class ActionCreationException extends Exception {

	public ActionCreationException() {
		super();
	}

	public ActionCreationException(String message) {
		super(message);
	}

	public ActionCreationException(Throwable cause) {
		super(cause);
	}

	public ActionCreationException(String message, Throwable cause) {
		super(message, cause);
	}
}
