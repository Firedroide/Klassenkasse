package ch.kanti_wohlen.klassenkasse.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf buf) throws Exception {
		if (packet == null) return;

		// Create a new temporary buffer for this packet
		ByteBuf writer = ctx.alloc().buffer();

		// Write header: ID and a placeholder for the length of the following data
		writer.writeByte(packet.getPacketId());
		writer.writeInt(0);

		// Write data
		packet.writeData(writer);

		// Write data length to previously set placeholder
		writer.setInt(1, writer.writerIndex() - 5);

		// Write packet terminator byte
		writer.writeByte(-1);

		// Write temporary buffer to network buffer
		buf.writeBytes(writer);
		writer.release();
	}
}
