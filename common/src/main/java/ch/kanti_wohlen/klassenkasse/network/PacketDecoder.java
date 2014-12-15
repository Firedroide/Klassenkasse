package ch.kanti_wohlen.klassenkasse.network;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDisconnect;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketHandshake;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class PacketDecoder extends ByteToMessageDecoder {

	private static final Logger LOGGER = Logger.getLogger(PacketDecoder.class.getSimpleName());

	private final Host host;

	public PacketDecoder(Host host) {
		this.host = host;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
		// If there are less bytes than the size of a packet with no data, this cannot yet be a full packet.
		// Therefore skip creating packets and let the output list empty.
		// This will result in the method being called again when more data is available
		// Due to network fragmentation, there might also arrive more than just the data of one packet
		// In that case, continue parsing packets until there's no data left.

		while (buf.readableBytes() >= Packet.PROTOCOL_OVERHEAD) {
			byte packetId = buf.readByte();
			int dataLength = buf.readInt();
			int readerIndexBeforeReading = buf.readerIndex();

			if (dataLength < 0) {
				throw new PacketCreationException("Length of data was lower than 0 (" + dataLength + ").");
			}

			// Not all data for this packet has arrived, abort and reset the buffer
			if (buf.readableBytes() < dataLength + 1) {
				buf.readerIndex(buf.readerIndex() - Packet.PROTOCOL_HEADER);
				break;
			}

			// Check if the packet ending byte exists
			byte controlByte = buf.getByte(buf.readerIndex() + dataLength);
			if (controlByte != (byte) -1) {
				throw new PacketCreationException("Control byte did not match (" + controlByte + " != -1) for packet of ID " + packetId + ".");
			}

			// Data seems to have been transmitted correctly, therefore...
			@SuppressWarnings("null")
			Packet packet = getPacketById(buf.readSlice(dataLength), packetId);

			// Check if all data was read, as it should have been
			if (buf.readerIndex() != readerIndexBeforeReading + dataLength) {
				throw new PacketCreationException("Not all data of the incoming data for a packet of ID " + packetId + " was read.");
			}

			// Increment read index to skip packet end byte
			// (which was already checked to be correct before)
			buf.skipBytes(1);

			if (packet == null) {
				throw new PacketCreationException("Packet parser returned null for packet of ID " + packetId + ").");
			}

			// Add packet to output for further processing in the pipeline.
			LOGGER.info("Decoded packet " + packet.getClass().getSimpleName());
			out.add(packet);

			// Discard read bytes of the just parsed packet
			buf.discardReadBytes();
		}
	}

	@SuppressWarnings("null")
	private Packet getPacketById(@NonNull ByteBuf data, byte packetId) throws PacketCreationException {
		Class<? extends Packet> packetClass = Protocol.getPacketClassById(packetId);
		if (packetClass == null) {
			throw new PacketCreationException("Unknown packet ID (" + packetId + ").");
		}

		Packet resultPacket = null;
		try {
			resultPacket = packetClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// Unit-Tested, won't happen.
			e.printStackTrace();
		}
		if (resultPacket == null) {
			throw new IllegalStateException("Packet constructor returned null value");
		}

		try {
			if (host == null && !(resultPacket instanceof PacketHandshake || resultPacket instanceof PacketDisconnect)) {
				throw new NullPointerException("Host");
			}

			resultPacket.readData(data, host);
		} catch (IndexOutOfBoundsException e) {
			// Rethrow
			throw new PacketCreationException("Ran out of data while creating packet with ID " + packetId + ".", e);
		}

		return resultPacket;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		if (cause instanceof PacketCreationException) {
			// Log exception
			cause.printStackTrace();
			// Reset connection
			ctx.close();
		} else {
			// Pass on the exception to the original handler
			super.exceptionCaught(ctx, cause);
		}
	}
}
