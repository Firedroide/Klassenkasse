package ch.kanti_wohlen.klassenkasse.network.packet;

import io.netty.buffer.ByteBuf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.PacketCreationException;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

@PacketType(Way.BOTH_WAYS)
public class PacketClassVariables extends Packet {

	private int classId;
	private Map<String, String> values;

	public PacketClassVariables() {
		values = Collections.emptyMap();
	}

	public PacketClassVariables(int classId, Map<String, String> values) {
		this.classId = classId;
		this.values = Collections.unmodifiableMap(new HashMap<>(values));
	}

	public int getClassId() {
		return classId;
	}

	public Map<String, String> getVariables() {
		return values;
	}

	@Override
	public void readData(ByteBuf buf, Host host) throws PacketCreationException {
		classId = buf.readInt();

		Map<String, String> result = new HashMap<>();
		while (buf.isReadable()) {
			String key = BufferUtil.readString(buf);
			String value = BufferUtil.readString(buf);
			result.put(key, value);
		}
		values = Collections.unmodifiableMap(result);
	}

	@Override
	public void writeData(ByteBuf buf) {
		if (values == null) {
			throw new IllegalStateException("Request type for the request was not set.");
		}

		buf.writeInt(classId);
		for (Entry<String, String> entry : values.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (key == null || key.isEmpty()) {
				throw new IllegalArgumentException("Key cannot be empty or null.");
			} else if (key.length() > 32) {
				throw new IllegalArgumentException("Key length cannot be greater than 32.");
			}

			BufferUtil.writeString(buf, key);
			BufferUtil.writeString(buf, value);
		}
	}
}
