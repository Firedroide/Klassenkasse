package ch.kanti_wohlen.klassenkasse.action.classes;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.ActionCreationException;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

public class ActionClassUpdated extends ActionClass {

	private @Nullable String name;

	public ActionClassUpdated(@NonNull Host host, StudentClass studentClass, String newName) {
		super(host, studentClass);

		name = (newName != null) ? newName : studentClass.getName();
	}

	public ActionClassUpdated(@NonNull Host host) {
		super(host);
	}

	public ActionClassUpdated(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	@Override
	public void readData(ByteBuf buf, Host host) throws ActionCreationException {
		int classId = host.getIdMapper().getClassMapping(buf.readInt());
		studentClass = host.getClassById(classId);
		if (studentClass == null) {
			throw new ActionCreationException("Inexistant class (id = " + classId + ")");
		}

		name = BufferUtil.readString(buf);
	}

	@Override
	public void apply(Host host) {
		StudentClass studentClass = assertNotNull(this.studentClass);
		checkState(false);

		swap(studentClass);
		host.updateClass(studentClass, false);
	}

	@Override
	public void undo(Host host) {
		StudentClass studentClass = assertNotNull(this.studentClass);
		checkState(true);

		swap(studentClass);
		host.updateClass(studentClass, false);
	}

	public @NonNull StudentClass getUpdatedStudentClass() {
		StudentClass studentClass = assertNotNull(this.studentClass);
		String name = assertNotNull(this.name);

		return new StudentClass(studentClass.getLocalId(), name, studentClass.getRoundingValue(), studentClass.getBalance());
	}

	private void swap(@NonNull StudentClass studentClass) {
		String name = assertNotNull(this.name);

		applied = !applied;

		String oldName = studentClass.getName();
		studentClass.setName(name);
		this.name = oldName;
	}
}
