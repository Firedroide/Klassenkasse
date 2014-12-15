package ch.kanti_wohlen.klassenkasse.network.packet;

import io.netty.buffer.ByteBuf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketRoles extends Packet {

	private Map<Integer, Role> roles;

	public PacketRoles() {
		roles = Collections.emptyMap();
	}

	public PacketRoles(Map<Integer, Role> roles) {
		this.roles = Collections.unmodifiableMap(new HashMap<>(roles));
	}

	public Map<Integer, Role> getRoles() {
		return roles;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		Map<Integer, Role> resultMap = new HashMap<>();
		while (buf.isReadable()) {
			int roleId = buf.readInt();
			String name = BufferUtil.readString(buf);
			String permissions = BufferUtil.readString(buf);

			resultMap.put(roleId, new Role(roleId, name, permissions));
		}

		roles = Collections.unmodifiableMap(resultMap);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (Role role : roles.values()) {
			if (role == null) continue;

			buf.writeInt(role.getLocalId());
			BufferUtil.writeString(buf, role.getName());
			BufferUtil.writeString(buf, role.getPermissionsString());
		}
	}
}
