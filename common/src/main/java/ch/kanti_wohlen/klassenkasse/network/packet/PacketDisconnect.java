package ch.kanti_wohlen.klassenkasse.network.packet;

import io.netty.buffer.ByteBuf;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.PacketCreationException;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;

/**
 * A {@code PacketDisconnect} is sent from the client to the server to indicate
 * that the connection should be shut down.
 * <p>
 * It does not contain any additional data to be sent.
 * </p>
 */
@PacketType(Way.CLIENT_TO_SERVER)
public class PacketDisconnect extends Packet {

	public PacketDisconnect() {}

	@Override
	public void readData(ByteBuf buf, Host host) throws PacketCreationException {}

	@Override
	public void writeData(ByteBuf buf) {}

}
