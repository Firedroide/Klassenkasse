package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@PacketType(Way.BOTH_WAYS)
public class PacketActionCommitted extends Packet {

	private ByteBuf buffer;

	public PacketActionCommitted() {
		buffer = null; // Initialize when needed
	}

	@Override
	public void readData(ByteBuf buf) {
		buffer = buf.copy();
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeBytes(buffer);
		buffer.release();
	}

	public ByteBuf getBuffer() {
		if (buffer == null) buffer = Unpooled.buffer();
		return buffer;
	}
}
