package ch.kanti_wohlen.klassenkasse.util;

public class IllegalConfigurationAccessException extends RuntimeException {

	public IllegalConfigurationAccessException() {
		super();
	}

	public IllegalConfigurationAccessException(String message) {
		super(message);
	}

	public IllegalConfigurationAccessException(Throwable cause) {
		super(cause);
	}

	public IllegalConfigurationAccessException(String message, Throwable cause) {
		super(message, cause);
	}
}
