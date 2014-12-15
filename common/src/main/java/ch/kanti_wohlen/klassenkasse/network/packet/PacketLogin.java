package ch.kanti_wohlen.klassenkasse.network.packet;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(PacketType.Way.CLIENT_TO_SERVER)
public class PacketLogin extends Packet {

	private String usr;
	private char[] pwd;
	private boolean token;

	public PacketLogin() {}

	public PacketLogin(String username, char[] password, boolean isToken) {
		// Make sure no NULL characters or whitespaces are smuggled in
		usr = username.trim();
		pwd = password;
		token = isToken;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		usr = BufferUtil.readString(buf);
		token = buf.readBoolean();
		pwd = new char[buf.readableBytes() / 2];
		for (int i = 0; buf.isReadable(2); ++i) {
			pwd[i] = buf.readChar();
		}
	}

	@Override
	public void writeData(ByteBuf buf) {
		BufferUtil.writeString(buf, usr);
		buf.writeBoolean(token);
		for (char c : pwd) {
			buf.writeChar(c);
		}
	}

	public String getUsername() {
		return usr;
	}

	public void setUsername(String username) {
		usr = username;
	}

	public char[] getPassword() {
		return pwd;
	}

	public void setPassword(char[] password) {
		pwd = password;
	}

	public boolean isTokenLogin() {
		return token;
	}

	public void setTokenLogin(boolean isTokenLogin) {
		token = isTokenLogin;
	}
}
