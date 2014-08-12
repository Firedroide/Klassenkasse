package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import io.netty.buffer.ByteBuf;

@PacketType(Way.BOTH_WAYS)
public class PacketHandshake extends Packet {

	private short protocolVersion;
	private boolean https;

	public PacketHandshake() {}

	public PacketHandshake(short protocolVersion, boolean https) {
		this.protocolVersion = protocolVersion;
		this.https = https;
	}

	public short getProtocolVersion() {
		return protocolVersion;
	}

	public boolean usingHttps() {
		return https;
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeShort(protocolVersion);
		buf.writeBoolean(https);
	}

	@Override
	public void readData(ByteBuf buf) {
		protocolVersion = buf.readShort();
		https = buf.readBoolean();
	}
}
