package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketAccepted extends Packet {

	public enum Type {
		CLASS, USER, PAYMENT;
	}

	private Type dataType;
	private int clientId;
	private int serverId;

	public PacketAccepted() {}

	public PacketAccepted(Type dataType, int clientId, int serverId) {
		this.dataType = dataType;
		this.clientId = clientId;
		this.serverId = serverId;
	}

	public int getClientId() {
		return clientId;
	}

	public int getServerId() {
		return serverId;
	}

	@Override
	public void readData(ByteBuf buf) {
		dataType = BufferUtil.readEnum(buf, Type.values());
		clientId = buf.readInt();
		serverId = buf.readInt();
	}

	@Override
	public void writeData(ByteBuf buf) {
		buf.writeByte(dataType.ordinal());
		buf.writeInt(clientId);
		buf.writeInt(serverId);
	}
}
