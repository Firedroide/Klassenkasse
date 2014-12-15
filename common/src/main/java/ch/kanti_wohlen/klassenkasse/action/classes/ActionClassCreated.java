package ch.kanti_wohlen.klassenkasse.action.classes;

import io.netty.buffer.ByteBuf;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@NonNullByDefault
public class ActionClassCreated extends ActionClass {

	public ActionClassCreated(StudentClass studentClass) {
		super(studentClass, UpdateType.CREATION);
		checkStudentClass(studentClass);
	}

	private static void checkStudentClass(StudentClass studentClass) {
		if (!studentClass.getRoundedBalance().equals(MonetaryValue.ZERO)) {
			throw new IllegalArgumentException("Cannot create a StudentClass with a balance other than 0");
		} else if (!studentClass.getRoundingValue().equals(MonetaryValue.ZERO)) {
			throw new IllegalArgumentException("Cannot create a StudentClass with a rounding value other than 0");
		}
	}

	public ActionClassCreated(Host host, ByteBuf buffer) {
		super(host, buffer, UpdateType.CREATION);
	}
}
