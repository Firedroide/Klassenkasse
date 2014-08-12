package ch.kanti_wohlen.klassenkasse.network;

import java.util.List;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.ActionCreationException;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketActionCommitted;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

public class ActionEncoder extends MessageToMessageEncoder<Action[]> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Action[] msgs, List<Object> out) throws Exception {
		if (msgs == null || msgs.length == 0) return;

		PacketActionCommitted packet = new PacketActionCommitted();
		ByteBuf buf = packet.getBuffer();
		for (int i = 0; i < msgs.length; ++i) {
			Action action = msgs[i];

			// Action header: Action type ID and length placeholder
			buf.writeByte(action.getActionId());
			int lengthIndex = buf.writerIndex();
			buf.writeShort(0);

			// Write data: Action ID and custom Action data
			buf.writeLong(action.getLocalId());
			int startIndex = buf.writerIndex();
			action.writeData(buf);

			// Set length before data
			int dataLength = buf.writerIndex() - startIndex;
			if (dataLength > Short.MAX_VALUE) {
				throw new ActionCreationException("Too much data for one action (" + dataLength + " > 32767)");
			}
			buf.setShort(lengthIndex, dataLength);

			if (i < msgs.length - 1) {
				buf.writeByte(-3); // Still more actions after this one.
			} else {
				buf.writeByte(-2); // Packet list termination byte
			}
		}
		out.add(packet);
	}
}
