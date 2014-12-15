package ch.kanti_wohlen.klassenkasse.action.users;

import io.netty.buffer.ByteBuf;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;

@NonNullByDefault
public class ActionUserRemoved extends ActionUser {

	public ActionUserRemoved(User user) {
		super(user, UpdateType.REMOVAL);
	}

	public ActionUserRemoved(Host host, ByteBuf buffer) {
		super(host, buffer, UpdateType.REMOVAL);
	}
}
