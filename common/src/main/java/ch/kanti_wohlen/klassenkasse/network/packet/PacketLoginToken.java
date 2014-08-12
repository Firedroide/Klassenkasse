package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketLoginToken extends Packet {

	private String token;

	public PacketLoginToken() {}

	public PacketLoginToken(String loginToken) {
		token = loginToken;
	}

	public String getToken() {
		return token;
	}

	@Override
	public void readData(ByteBuf buf) {
		token = BufferUtil.readString(buf);
	}

	@Override
	public void writeData(ByteBuf buf) {
		BufferUtil.writeString(buf, token);
	}
}
