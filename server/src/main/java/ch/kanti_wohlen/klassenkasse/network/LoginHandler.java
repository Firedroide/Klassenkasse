package ch.kanti_wohlen.klassenkasse.network;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDisconnect;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketLogin;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class LoginHandler extends MessageToMessageDecoder<Packet> {

	private static final byte LOGIN_PACKET_ID = Protocol.getPacketId(PacketLogin.class);
	private static final byte DISCONNECT_PACKET_ID = Protocol.getPacketId(PacketDisconnect.class);

	private static final Map<String, Long> LOGIN_ATTEMPTS = new HashMap<>();

	private final Host host;

	public LoginHandler(Host host) {
		this.host = host;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, Packet packet, List<Object> out) throws Exception {
		if (host.getLoggedInUser() == null) {
			if (packet.getPacketId() != LOGIN_PACKET_ID && packet.getPacketId() != DISCONNECT_PACKET_ID) {
				throw new LoginException("User is not yet logged in!");
			} else {
				// Make sure this user is not spamming login packets
				String ip = getIPFromRemoteAddress(ctx);
				Long lastLoginAttempt = LOGIN_ATTEMPTS.get(ip);
				LOGIN_ATTEMPTS.put(ip, System.currentTimeMillis());

				// Only one login attempt every 3 seconds is allowed
				if (lastLoginAttempt != null && lastLoginAttempt > System.currentTimeMillis() - 3000) {
					return;
				}
			}
		} else {
			if (packet.getPacketId() == LOGIN_PACKET_ID) {
				throw new LoginException("User is already logged in!");
			}
		}

		// Pass packet through
		out.add(packet);
	}

	private static String getIPFromRemoteAddress(ChannelHandlerContext ctx) {
		InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
		return address.getHostString();
	}
}
