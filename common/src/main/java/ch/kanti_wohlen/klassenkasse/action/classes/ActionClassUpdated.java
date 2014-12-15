package ch.kanti_wohlen.klassenkasse.action.classes;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;

@NonNullByDefault
public class ActionClassUpdated extends ActionClass {

	public ActionClassUpdated(StudentClass studentClass) {
		super(studentClass, UpdateType.UPDATE);
	}

	public ActionClassUpdated(Host host, ByteBuf buffer) {
		super(host, buffer, UpdateType.UPDATE);
	}
}
