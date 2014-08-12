package ch.kanti_wohlen.klassenkasse.action.users;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.ActionCreationException;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

public class ActionUserUpdated extends ActionUser {

	private int classId;
	private int roleId;
	private @Nullable String firstName;
	private @Nullable String lastName;
	private @Nullable String eMailAddress;

	public ActionUserUpdated(@NonNull Host host, User user, StudentClass newClass, Role newRole, String newFirstName, String newLastName, String newEmailAddress) {
		super(host, user);

		classId = (newClass != null) ? newClass.getLocalId() : user.getStudentClassId();
		roleId = (newRole != null) ? newRole.getLocalId() : user.getRoleId();
		firstName = (newFirstName != null) ? newFirstName : user.getFirstName();
		lastName = (newLastName != null) ? newLastName : user.getLastName();
		eMailAddress = (newEmailAddress != null) ? newEmailAddress : user.getEMailAddress();
	}

	public ActionUserUpdated(@NonNull Host host) {
		super(host);
	}

	public ActionUserUpdated(long id) {
		super(id);
	}

	@Override
	public void readData(ByteBuf buf, Host host, IdMapper idMapper) throws ActionCreationException {
		int userId = idMapper.getUserMapping(buf.readInt());
		user = host.getUserById(userId);
		if (user == null) {
			throw new ActionCreationException("Inexistant user (id = " + userId + ")");
		}

		classId = idMapper.getClassMapping(buf.readInt());
		roleId = buf.readInt();
		firstName = BufferUtil.readString(buf);
		lastName = BufferUtil.readString(buf);
		eMailAddress = BufferUtil.readString(buf);
	}

	@Override
	public void apply(Host host) {
		User user = assertNotNull(this.user);
		checkState(false);

		swap(user);
		host.updateUser(user, true);
	}

	@Override
	public void undo(Host host) {
		User user = assertNotNull(this.user);
		checkState(true);

		swap(user);
		host.updateUser(user, true);
	}

	private void swap(@NonNull User user) {
		applied = !applied;

		int oldClassId = user.getStudentClassId();
		int oldRoleId = user.getRoleId();
		String oldFirstName = user.getFirstName();
		String oldLastName = user.getLastName();
		String oldEMailAddress = user.getEMailAddress();

		String firstName = assertNotNull(this.firstName);
		String lastName = assertNotNull(this.lastName);
		String eMailAddress = assertNotNull(this.eMailAddress);

		user.setStudentClass(classId);
		user.setRole(roleId);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmailAddress(eMailAddress);

		this.classId = oldClassId;
		this.roleId = oldRoleId;
		this.firstName = oldFirstName;
		this.lastName = oldLastName;
		this.eMailAddress = oldEMailAddress;
	}
}
