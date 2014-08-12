package ch.kanti_wohlen.klassenkasse.action.users;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;

public class ActionUserRemoved extends ActionUser {

	public ActionUserRemoved(@NonNull Host host, User user) {
		super(host, user);
	}

	public ActionUserRemoved(@NonNull Host host) {
		super(host);
	}

	public ActionUserRemoved(long id) {
		super(id);
	}

	@Override
	public void readData(ByteBuf buf, Host host, IdMapper idMapper) {
		int userId = idMapper.getUserMapping(buf.readInt());
		user = host.getUserById(userId);
	}

	@Override
	public void writeData(ByteBuf buf) {
		User user = assertNotNull(this.user);
		buf.writeInt(user.getLocalId());
	}

	@Override
	public void apply(Host host) {
		User user = assertNotNull(this.user);
		checkState(false);

		host.updateUser(user, true);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		User user = assertNotNull(this.user);
		checkState(true);

		host.updateUser(user, false);
		applied = false;
	}
}
