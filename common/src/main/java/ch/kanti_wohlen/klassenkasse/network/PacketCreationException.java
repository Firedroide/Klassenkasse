package ch.kanti_wohlen.klassenkasse.network;

public class PacketCreationException extends Exception {

	public PacketCreationException() {
		super();
	}

	public PacketCreationException(String message) {
		super(message);
	}

	public PacketCreationException(Throwable cause) {
		super(cause);
	}

	public PacketCreationException(String message, Throwable cause) {
		super(message, cause);
	}
}
