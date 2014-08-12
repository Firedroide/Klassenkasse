package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.kanti_wohlen.klassenkasse.framework.Role;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketRoles extends Packet {

	private Collection<Role> roles;

	public PacketRoles() {
		roles = Collections.emptyList();
	}

	public PacketRoles(Role... roles) {
		this.roles = Arrays.asList(roles);
	}

	public PacketRoles(Collection<Role> roles) {
		this.roles = new ArrayList<>(roles);
	}

	public Collection<Role> getRoles() {
		return Collections.unmodifiableCollection(roles);
	}

	@Override
	public void readData(ByteBuf buf) {
		List<Role> resultList = new ArrayList<>();
		while (buf.isReadable()) {
			int roleId = buf.readInt();
			String name = BufferUtil.readString(buf);
			String permissions = BufferUtil.readString(buf);
			resultList.add(new Role(roleId, name, permissions));
		}
		roles = Collections.unmodifiableList(resultList);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (Role role : roles) {
			buf.writeInt(role.getLocalId());
			BufferUtil.writeString(buf, role.getName());
			BufferUtil.writeString(buf, role.getAllPermissions());
		}
	}
}
