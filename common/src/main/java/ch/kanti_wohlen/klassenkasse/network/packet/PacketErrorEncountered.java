package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.Protocol.NetworkError;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketErrorEncountered extends Packet {

	private NetworkError networkError;
	private String reason;

	public PacketErrorEncountered() {
		networkError = NetworkError.UNKNOWN_ERROR;
	}

	public PacketErrorEncountered(NetworkError networkError, String reason) {
		this.networkError = networkError;
		this.reason = reason;
	}

	public static PacketErrorEncountered fromNetworkError(NetworkError networkError) {
		return new PacketErrorEncountered(networkError, null);
	}

	public static PacketErrorEncountered unknownError(String reason) {
		return new PacketErrorEncountered(NetworkError.UNKNOWN_ERROR, reason);
	}

	public NetworkError getNetworkError() {
		return networkError;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		networkError = BufferUtil.readEnum(buf, NetworkError.values());
		reason = BufferUtil.readString(buf);
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeByte(networkError.ordinal());
		BufferUtil.writeString(buf, reason);
	}
}
