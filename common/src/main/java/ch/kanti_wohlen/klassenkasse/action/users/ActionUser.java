package ch.kanti_wohlen.klassenkasse.action.users;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

public abstract class ActionUser extends Action {

	protected @Nullable User user;

	public ActionUser(@NonNull Host host, User user) {
		super(host);
		this.user = user;
	}

	public ActionUser(@NonNull Host host) {
		super(host);
	}

	public ActionUser(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	public User getUser() {
		return user;
	}

	@Override
	public void writeData(ByteBuf buf) {
		User user = assertNotNull(this.user);

		buf.writeInt(user.getLocalId());
		buf.writeInt(user.getStudentClassId());
		buf.writeInt(user.getRoleId());
		BufferUtil.writeString(buf, user.getFirstName());
		BufferUtil.writeString(buf, user.getLastName());
		BufferUtil.writeString(buf, user.getEMailAddress());
	}
}
