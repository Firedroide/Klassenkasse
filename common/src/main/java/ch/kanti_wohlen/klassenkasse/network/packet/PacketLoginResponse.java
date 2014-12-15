package ch.kanti_wohlen.klassenkasse.network.packet;

import io.netty.buffer.ByteBuf;

import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketLoginResponse extends Packet {

	private String token;

	public PacketLoginResponse() {}

	public PacketLoginResponse(String loginToken) {
		token = loginToken;
	}

	public @Nullable String getToken() {
		return token;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		if (buf.isReadable()) {
			token = BufferUtil.readString(buf);
		} else {
			token = null;
		}
	}

	@Override
	public void writeData(ByteBuf buf) {
		if (token != null) {
			BufferUtil.writeString(buf, token);
		}
	}
}
