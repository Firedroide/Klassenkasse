package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketUsernames extends Packet {

	private Collection<String> usernames;

	public PacketUsernames() {
		usernames = Collections.emptyList();
	}

	public PacketUsernames(String... usernames) {
		this.usernames = Collections.unmodifiableCollection(Arrays.asList(usernames));
	}

	public PacketUsernames(Collection<String> usernames) {
		this.usernames = Collections.unmodifiableCollection(usernames);
	}

	public Collection<String> getUsernames() {
		return usernames;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		List<String> result = new ArrayList<>();
		while (buf.isReadable()) {
			result.add(BufferUtil.readString(buf));
		}
		usernames = Collections.unmodifiableList(result);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (String username : usernames) {
			BufferUtil.writeString(buf, username);
		}
	}
}
