package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketStudentClasses extends Packet {

	private Map<Integer, StudentClass> studentClasses;

	public PacketStudentClasses() {
		studentClasses = Collections.emptyMap();
	}

	public PacketStudentClasses(Map<Integer, StudentClass> studentClasses) {
		this.studentClasses = Collections.unmodifiableMap(new HashMap<>(studentClasses));
	}

	public Map<Integer, StudentClass> getStudentClasses() {
		return studentClasses;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		Map<Integer, StudentClass> resultMap = new HashMap<>();
		while (buf.isReadable()) {
			int studentId = buf.readInt();
			String name = BufferUtil.readString(buf);
			MonetaryValue rounding = new MonetaryValue(buf.readLong());
			MonetaryValue balance = new MonetaryValue(buf.readLong());

			resultMap.put(studentId, new StudentClass(studentId, name, rounding, balance));
		}

		studentClasses = Collections.unmodifiableMap(resultMap);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (StudentClass studentClass : studentClasses.values()) {
			if (studentClass == null) continue;

			buf.writeInt(studentClass.getLocalId());
			BufferUtil.writeString(buf, studentClass.getName());
			buf.writeLong(studentClass.getRoundingValue().getCentValue());
			buf.writeLong(studentClass.getRawBalance().getCentValue());
		}
	}
}
