package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketUsers extends Packet {

	private Map<Integer, User> users;

	public PacketUsers() {
		users = Collections.emptyMap();
	}

	public PacketUsers(Map<Integer, User> users) {
		this.users = Collections.unmodifiableMap(new HashMap<>(users));
	}

	public Map<Integer, User> getUsers() {
		return users;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		Map<Integer, User> resultMap = new HashMap<>();
		while (buf.isReadable()) {
			int userId = buf.readInt();
			int studentClassId = buf.readInt();
			int roleId = buf.readInt();
			String firstName = BufferUtil.readString(buf);
			String lastName = BufferUtil.readString(buf);
			String username = BufferUtil.readString(buf);
			MonetaryValue balance = new MonetaryValue(buf.readLong());

			resultMap.put(userId, new User(userId, studentClassId, roleId, firstName, lastName, username, balance));
		}

		users = Collections.unmodifiableMap(resultMap);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (User user : users.values()) {
			if (user == null) continue;

			buf.writeInt(user.getLocalId());
			buf.writeInt(user.getStudentClassId());
			buf.writeInt(user.getRoleId());
			BufferUtil.writeString(buf, user.getFirstName());
			BufferUtil.writeString(buf, user.getLastName());
			BufferUtil.writeString(buf, user.getUsername());
			buf.writeLong(user.getBalance().getCentValue());
		}
	}
}
