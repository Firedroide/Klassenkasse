package ch.kanti_wohlen.klassenkasse.action.users;

import io.netty.buffer.ByteBuf;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public abstract class ActionUser extends Action {

	protected final UpdateType type;
	protected final int userId;

	protected int classId;
	protected int roleId;
	protected String firstName;
	protected String lastName;
	protected String username;

	public ActionUser(User user, UpdateType type) {
		this.type = type;
		this.userId = user.getLocalId();

		this.classId = user.getStudentClassId();
		this.roleId = user.getRoleId();
		this.firstName = user.getFirstName();
		this.lastName = user.getLastName();
		this.username = user.getUsername();
	}

	public ActionUser(Host host, ByteBuf buffer, UpdateType type) {
		this.type = type;

		IdMapper idMapper = host.getIdMapper();

		int clientId = buffer.readInt();
		this.classId = idMapper.getClassMapping(buffer.readInt());
		this.roleId = buffer.readInt();
		this.firstName = BufferUtil.readString(buffer);
		this.lastName = BufferUtil.readString(buffer);
		this.username = BufferUtil.readString(buffer);

		if (type == UpdateType.CREATION) {
			if (clientId < 0) { // TODO: Better solution?
				this.userId = host.getIdProvider().generateUserId();
				idMapper.mapUser(clientId, this.userId);
			} else {
				this.userId = clientId;
			}
		} else {
			this.userId = host.getIdMapper().getUserMapping(clientId);
		}
	}

	public int getUserId() {
		return userId;
	}

	public int getClassId() {
		return classId;
	}

	public int getRoleId() {
		return roleId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getUsername() {
		return username;
	}

	@Override
	protected void update(Host host, boolean apply) {
		User current = host.getUserById(userId);
		UpdateType actualType = apply ? type : type.reverse();
		checkState(current, actualType);

		MonetaryValue balance;
		if (current != null) {
			balance = current.getBalance();
		} else {
			balance = MonetaryValue.ZERO;
		}

		User update = new User(userId, classId, roleId, firstName, lastName, username, balance);

		host.updateUser(update, actualType);

		if (current != null) {
			classId = current.getStudentClassId();
			roleId = current.getRoleId();
			firstName = current.getFirstName();
			lastName = current.getLastName();
			username = current.getUsername();
		}
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeInt(userId);
		buf.writeInt(classId);
		buf.writeInt(roleId);
		BufferUtil.writeString(buf, firstName);
		BufferUtil.writeString(buf, lastName);
		BufferUtil.writeString(buf, username);
	}
}
