package ch.kanti_wohlen.klassenkasse.action.users;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class ActionUserCreated extends ActionUser {

	private boolean isRestore = false;

	public ActionUserCreated(@NonNull Host host, User user) {
		super(host, user);
	}

	public ActionUserCreated(@NonNull Host host) {
		super(host);
	}

	public ActionUserCreated(long id) {
		super(id);
		isRestore = true;
	}

	@Override
	public void readData(ByteBuf buf, Host host, IdMapper idMapper) {
		int clientUserId = buf.readInt();
		int classId = idMapper.getClassMapping(buf.readInt());
		int roleId = buf.readInt();
		String firstName = BufferUtil.readString(buf);
		String lastName = BufferUtil.readString(buf);
		String eMail = BufferUtil.readString(buf);
		MonetaryValue value = new MonetaryValue(buf.readLong());

		if (isRestore) {
			user = new User(clientUserId, classId, roleId, firstName, lastName, eMail, value);
		} else {
			user = new User(host, classId, roleId, firstName, lastName, eMail);
			idMapper.mapUser(clientUserId, user.getLocalId());
		}
	}

	@Override
	public void writeData(ByteBuf buf) {
		User user = assertNotNull(this.user);
		super.writeData(buf);
		buf.writeLong(user.getBalance().getCentValue());
	}

	@Override
	public void apply(Host host) {
		User user = assertNotNull(this.user);
		checkState(false);

		host.updateUser(user, false);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		User user = assertNotNull(this.user);
		checkState(true);

		host.updateUser(user, true);
		applied = false;
	}
}
