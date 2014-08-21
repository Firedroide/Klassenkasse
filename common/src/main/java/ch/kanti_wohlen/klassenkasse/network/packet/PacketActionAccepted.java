package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketActionAccepted extends Packet {

	private long[] clientIds;
	private long[] serverIds;

	public PacketActionAccepted() {}

	public PacketActionAccepted(long[] clientIds, long[] serverIds) {
		this.clientIds = clientIds;
		this.serverIds = serverIds;
	}

	public PacketActionAccepted(Collection<Entry<Long, Long>> mappings) {
		readMappings(mappings);
	}

	private void readMappings(Collection<Entry<Long, Long>> mappings) {
		clientIds = new long[mappings.size()];
		serverIds = new long[mappings.size()];

		int i = 0;
		for (Entry<Long, Long> mapping : mappings) {
			clientIds[i] = mapping.getKey();
			serverIds[i] = mapping.getValue();
			++i;
		}
	}

	public long[] getClientIds() {
		return clientIds;
	}

	public long[] getServerIds() {
		return serverIds;
	}

	@Override
	public void readData(ByteBuf buf) {
		List<Entry<Long, Long>> mappings = new ArrayList<>();

		while (buf.isReadable(8)) {
			long clientId = buf.readLong();
			long serverId = buf.readLong();
			mappings.add(new SimpleEntry<>(clientId, serverId));
		}
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (int i = 0; i < clientIds.length; ++i) {
			buf.writeLong(clientIds[i]);
			buf.writeLong(serverIds[i]);
		}
	}
}
