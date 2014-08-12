package ch.kanti_wohlen.klassenkasse.action.classes;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;

public class ActionClassRemoved extends ActionClass {

	public ActionClassRemoved(@NonNull Host host, StudentClass studentClass) {
		super(host, studentClass);
	}

	public ActionClassRemoved(@NonNull Host host) {
		super(host);
	}

	public ActionClassRemoved(long id) {
		super(id);
	}

	@Override
	public void readData(ByteBuf buf, Host host, IdMapper idMapper) {
		int classId = idMapper.getClassMapping(buf.readInt());
		studentClass = host.getClassById(classId);
	}

	@Override
	public void writeData(ByteBuf buf) {
		StudentClass studentClass = assertNotNull(this.studentClass);
		buf.writeInt(studentClass.getLocalId());
	}

	@Override
	public void apply(Host host) {
		StudentClass studentClass = assertNotNull(this.studentClass);
		checkState(false);

		host.updateClass(studentClass, true);
		applied = true;
	}

	@Override
	public void undo(Host host) {
		StudentClass studentClass = assertNotNull(this.studentClass);
		checkState(true);

		host.updateClass(studentClass, false);
		applied = false;
	}
}
