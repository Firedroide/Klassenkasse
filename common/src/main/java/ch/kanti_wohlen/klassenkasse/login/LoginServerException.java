package ch.kanti_wohlen.klassenkasse.login;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.network.Protocol.NetworkError;

@NonNullByDefault
public class LoginServerException extends Exception {

	private final NetworkError networkError;

	public LoginServerException() {
		super();
		this.networkError = NetworkError.INVALID_LOGIN;
	}

	public LoginServerException(String message, Throwable cause) {
		super(message, cause);
		this.networkError = NetworkError.UNKNOWN_ERROR;
	}

	public LoginServerException(Throwable cause) {
		super(cause);
		this.networkError = NetworkError.UNKNOWN_ERROR;
	}

	public NetworkError getNetworkError() {
		return networkError;
	}
}
