package ch.kanti_wohlen.klassenkasse.network.packet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // For testing, should probably change
@Target(ElementType.TYPE)
public @interface PacketType {

	Way value();

	static enum Way {
		BOTH_WAYS,
		CLIENT_TO_SERVER,
		SERVER_TO_CLIENT
	}
}
