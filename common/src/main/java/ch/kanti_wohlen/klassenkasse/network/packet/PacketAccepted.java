package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketAccepted extends Packet {

	public PacketAccepted() {}

	@Override
	public void readData(ByteBuf buf, Host host) {}

	@Override
	public void writeData(ByteBuf buf) {}
}
