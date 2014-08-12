package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketUsers extends Packet {

	private Collection<User> users;

	public PacketUsers() {
		users = Collections.emptyList();
	}

	public PacketUsers(User... users) {
		this.users = Arrays.asList(users);
	}

	public PacketUsers(Collection<User> users) {
		this.users = new ArrayList<>(users);
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users);
	}

	@Override
	public void readData(ByteBuf buf) {
		List<User> resultList = new ArrayList<>();
		while (buf.isReadable()) {
			int userId = buf.readInt();
			int studentClassId = buf.readInt();
			int roleId = buf.readInt();
			String firstName = BufferUtil.readString(buf);
			String lastName = BufferUtil.readString(buf);
			String eMailAddress = BufferUtil.readString(buf);
			MonetaryValue balance = new MonetaryValue(buf.readLong());
			resultList.add(new User(userId, studentClassId, roleId, firstName, lastName, eMailAddress, balance));
		}
		users = Collections.unmodifiableList(resultList);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (User user : users) {
			buf.writeInt(user.getLocalId());
			buf.writeInt(user.getStudentClassId());
			buf.writeInt(user.getRoleId());
			BufferUtil.writeString(buf, user.getFirstName());
			BufferUtil.writeString(buf, user.getLastName());
			BufferUtil.writeString(buf, user.getEMailAddress());
			buf.writeLong(user.getBalance().getCentValue());
		}
	}
}
