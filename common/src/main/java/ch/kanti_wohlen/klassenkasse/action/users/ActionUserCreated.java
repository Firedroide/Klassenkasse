package ch.kanti_wohlen.klassenkasse.action.users;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class ActionUserCreated extends ActionUser {

	public ActionUserCreated(User user) {
		super(user, UpdateType.CREATION);
		checkUser(user);
	}

	private static void checkUser(User user) {
		if (!user.getBalance().equals(MonetaryValue.ZERO)) {
			throw new IllegalArgumentException("Cannot create a Payment with a balance other than 0");
		}
	}

	public ActionUserCreated(Host host, ByteBuf buffer) {
		super(host, buffer, UpdateType.CREATION);
	}
}
