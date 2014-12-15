package ch.kanti_wohlen.klassenkasse.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Arrays;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.login.LoginServerException;
import ch.kanti_wohlen.klassenkasse.network.Protocol.NetworkError;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketErrorEncountered;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketLogin;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketLoginResponse;
import ch.kanti_wohlen.klassenkasse.server.Server;

// TODO: Prevent login spamming
public class PacketLoginHandler extends SimpleChannelInboundHandler<PacketLogin> {

	private static final Logger LOGGER = Logger.getLogger(PacketLoginHandler.class.getSimpleName());
	private static final PacketErrorEncountered ERROR = PacketErrorEncountered.fromNetworkError(NetworkError.INVALID_LOGIN);

	private final @NonNull Host host;
	private final @NonNull User superUser;

	public PacketLoginHandler(@NonNull Host host) {
		User superUser = host.getUserById(0);
		if (superUser == null) throw new NullPointerException("SuperUser did not exist");

		this.host = host;
		this.superUser = superUser;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PacketLogin packet) throws Exception {
		LOGGER.info("Trying to log in user " + packet.getUsername());

		String username = packet.getUsername();
		char[] password = packet.getPassword();
		boolean isToken = packet.isTokenLogin();

		if (!isToken && isSuperUserLogin(username, password)) {
			ctx.writeAndFlush(new PacketLoginResponse());
			host.setLoggedInUser(superUser);
			return;
		}

		try {
			@SuppressWarnings("null")
			String token = host.getLoginProvider().logIn(host, username, password, isToken);
			PacketLoginResponse loginResponse = new PacketLoginResponse(token);
			ctx.writeAndFlush(loginResponse);
		} catch (LoginServerException e) {
			ctx.writeAndFlush(ERROR);
		}
	}

	private boolean isSuperUserLogin(String username, char[] password) {
		if (!superUser.getUsername().equals(username)) return false;

		char[] superUserPassword = Server.INSTANCE.getConfiguration().getString("authentication.superUserPassword").toCharArray();
		return Arrays.equals(password, superUserPassword);
	}
}
