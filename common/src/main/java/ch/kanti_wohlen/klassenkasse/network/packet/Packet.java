package ch.kanti_wohlen.klassenkasse.network.packet;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.PacketCreationException;
import ch.kanti_wohlen.klassenkasse.network.Protocol;
import io.netty.buffer.ByteBuf;

public abstract class Packet {

	public static final int PROTOCOL_HEADER = 5;
	public static final int PROTOCOL_FOOTER = 1;
	public static final int PROTOCOL_OVERHEAD = PROTOCOL_HEADER + PROTOCOL_FOOTER;

	public final byte getPacketId() {
		return Protocol.getPacketId(this.getClass());
	}

	public abstract void readData(@NonNull ByteBuf buf, @NonNull Host host) throws PacketCreationException;

	public abstract void writeData(@NonNull ByteBuf buf);
}
