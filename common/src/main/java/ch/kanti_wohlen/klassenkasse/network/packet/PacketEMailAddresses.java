package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketEMailAddresses extends Packet {

	private Collection<String> eMailAddresses;

	public PacketEMailAddresses() {
		eMailAddresses = Collections.emptyList();
	}

	public PacketEMailAddresses(String... eMails) {
		eMailAddresses = Arrays.asList(eMails);
	}

	public PacketEMailAddresses(Collection<String> eMails) {
		eMailAddresses = new ArrayList<>(eMails);
	}

	public Collection<String> getEMailAddresses() {
		return Collections.unmodifiableCollection(eMailAddresses);
	}

	@Override
	public void readData(ByteBuf buf) {
		List<String> result = new ArrayList<>();
		while (buf.isReadable()) {
			result.add(BufferUtil.readString(buf));
		}
		eMailAddresses = Collections.unmodifiableList(result);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (String eMail : eMailAddresses) {
			BufferUtil.writeString(buf, eMail);
		}
	}
}
