package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import io.netty.buffer.ByteBuf;

@PacketType(Way.BOTH_WAYS)
public class PacketHandshake extends Packet {

	private short protocolVersion;
	private boolean ssl;

	public PacketHandshake() {}

	public PacketHandshake(short protocolVersion, boolean useSSL) {
		this.protocolVersion = protocolVersion;
		this.ssl = useSSL;
	}

	public short getProtocolVersion() {
		return protocolVersion;
	}

	public boolean useSSL() {
		return ssl;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		protocolVersion = buf.readShort();
		ssl = buf.readBoolean();
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeShort(protocolVersion);
		buf.writeBoolean(ssl);
	}
}
