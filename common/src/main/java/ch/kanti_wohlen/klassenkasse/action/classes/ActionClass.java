package ch.kanti_wohlen.klassenkasse.action.classes;

import io.netty.buffer.ByteBuf;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public abstract class ActionClass extends Action {

	protected final UpdateType type;
	protected final int classId;

	protected String className;

	public ActionClass(StudentClass studentClass, UpdateType type) {
		this.type = type;
		this.classId = studentClass.getLocalId();

		this.className = studentClass.getName();
	}

	public ActionClass(Host host, ByteBuf buffer, UpdateType type) {
		this.type = type;

		int clientId = buffer.readInt();
		this.className = BufferUtil.readString(buffer);

		if (type == UpdateType.CREATION) {
			if (clientId < 0) { // TODO: Better solution?
				this.classId = host.getIdProvider().generateClassId();
				host.getIdMapper().mapClass(clientId, this.classId);
			} else {
				this.classId = clientId;
			}
		} else {
			this.classId = host.getIdMapper().getClassMapping(clientId);
		}
	}

	public int getStudentClassId() {
		return classId;
	}

	public String getStudentClassName() {
		return className;
	}

	@Override
	protected void update(Host host, boolean apply) {
		StudentClass current = host.getClassById(classId);
		UpdateType actualType = apply ? type : type.reverse();
		checkState(current, actualType);

		MonetaryValue balance;
		MonetaryValue rounding;
		if (actualType == UpdateType.UPDATE) {
			if (current == null) throw new IllegalStateException(); // checked

			balance = current.getRawBalance();
			rounding = current.getRoundingValue();
		} else {
			balance = MonetaryValue.ZERO;
			rounding = MonetaryValue.ZERO;
		}
		StudentClass update = new StudentClass(classId, className, rounding, balance);

		host.updateClass(update, actualType);

		if (current != null) {
			className = current.getName();
		}
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeInt(classId);
		BufferUtil.writeString(buf, className);
	}
}
