package ch.kanti_wohlen.klassenkasse.action.users;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;

@NonNullByDefault
public class ActionUserUpdated extends ActionUser {

	public ActionUserUpdated(User user) {
		super(user, UpdateType.UPDATE);
	}

	public ActionUserUpdated(Host host, ByteBuf buffer) {
		super(host, buffer, UpdateType.UPDATE);
	}
}
