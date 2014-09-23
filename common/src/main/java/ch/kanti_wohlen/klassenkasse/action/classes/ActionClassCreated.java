package ch.kanti_wohlen.klassenkasse.action.classes;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class ActionClassCreated extends ActionClass {

	private boolean isRestore = false;

	public ActionClassCreated(@NonNull Host host, StudentClass studentClass) {
		super(host, studentClass);
	}

	public ActionClassCreated(@NonNull Host host) {
		super(host);
	}

	@Deprecated
	public ActionClassCreated(long id, User creator, @NonNull Date date) {
		super(id, creator, date);
		isRestore = true;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		int clientClassId = buf.readInt();
		String className = BufferUtil.readString(buf);

		if (isRestore) {
			studentClass = new StudentClass(clientClassId, className, MonetaryValue.ZERO, MonetaryValue.ZERO);
		} else {
			StudentClass studentClass = new StudentClass(host, className);
			host.getIdMapper().mapClass(clientClassId, studentClass.getLocalId());
			this.studentClass = studentClass;
		}
	}

	@Override
	public void apply(Host host) {
		StudentClass studentClass = assertNotNull(this.studentClass);
		checkState(false);

		host.updateClass(studentClass, false);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		StudentClass studentClass = assertNotNull(this.studentClass);
		checkState(true);

		host.updateClass(studentClass, true);
		applied = false;
	}
}
