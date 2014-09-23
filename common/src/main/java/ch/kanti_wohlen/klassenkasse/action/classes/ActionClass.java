package ch.kanti_wohlen.klassenkasse.action.classes;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

public abstract class ActionClass extends Action {

	protected @Nullable StudentClass studentClass;

	public ActionClass(@NonNull Host host, StudentClass studentClass) {
		super(host);
		this.studentClass = studentClass;
	}

	public ActionClass(@NonNull Host host) {
		super(host);
	}

	public ActionClass(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
	}

	public StudentClass getStudentClass() {
		return studentClass;
	}

	@Override
	public void writeData(ByteBuf buf) {
		StudentClass studentClass = assertNotNull(this.studentClass);

		buf.writeInt(studentClass.getLocalId());
		BufferUtil.writeString(buf, studentClass.getName());
	}
}
