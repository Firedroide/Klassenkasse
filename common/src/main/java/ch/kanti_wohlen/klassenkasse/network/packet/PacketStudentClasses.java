package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketStudentClasses extends Packet {

	private Collection<StudentClass> studentClasses;

	public PacketStudentClasses() {
		studentClasses = Collections.emptyList();
	}

	public PacketStudentClasses(StudentClass... studentClasses) {
		this.studentClasses = Arrays.asList(studentClasses);
	}

	public PacketStudentClasses(Collection<StudentClass> studentClasses) {
		this.studentClasses = new ArrayList<>(studentClasses);
	}

	public Collection<StudentClass> getStudentClasses() {
		return Collections.unmodifiableCollection(studentClasses);
	}

	@Override
	public void readData(ByteBuf buf) {
		List<StudentClass> resultList = new ArrayList<>();
		while (buf.isReadable()) {
			int studentId = buf.readInt();
			String name = BufferUtil.readString(buf);
			MonetaryValue balance = new MonetaryValue(buf.readLong());
			resultList.add(new StudentClass(studentId, name, balance));
		}
		studentClasses = Collections.unmodifiableList(resultList);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (StudentClass studentClass : studentClasses) {
			buf.writeInt(studentClass.getLocalId());
			BufferUtil.writeString(buf, studentClass.getName());
			buf.writeLong(studentClass.getBalance().getCentValue());
		}
	}
}
