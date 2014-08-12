package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(PacketType.Way.CLIENT_TO_SERVER)
public class PacketLogin extends Packet {

	private String usr;
	private String pwd;
	private boolean token;

	public PacketLogin() {}

	public PacketLogin(String username, String password, boolean isToken) {
		// Make sure no NULL characters or whitespaces are smuggled in
		usr = username.trim();
		pwd = password;
		token = isToken;
	}

	@Override
	public void readData(ByteBuf buf) {
		usr = BufferUtil.readString(buf);
		pwd = BufferUtil.readString(buf);
		token = buf.readBoolean();
	}

	@Override
	public void writeData(ByteBuf buf) {
		BufferUtil.writeString(buf, usr);
		BufferUtil.writeString(buf, pwd);
		buf.writeBoolean(token);
	}

	public String getUsername() {
		return usr;
	}

	public void setUsername(String username) {
		usr = username;
	}

	public String getEncryptedPassword() {
		return pwd;
	}

	public void setEncryptedPassword(String password) {
		pwd = password;
	}

	public boolean isTokenLogin() {
		return token;
	}

	public void setTokenLogin(boolean isTokenLogin) {
		token = isTokenLogin;
	}
}
