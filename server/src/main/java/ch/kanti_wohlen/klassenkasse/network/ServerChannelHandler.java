package ch.kanti_wohlen.klassenkasse.network;

import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.framework.DatabaseHost;
import ch.kanti_wohlen.klassenkasse.network.handler.ErrorHandler;
import ch.kanti_wohlen.klassenkasse.network.handler.PacketActionCommittedHandler;
import ch.kanti_wohlen.klassenkasse.network.handler.PacketClassVariablesHandler;
import ch.kanti_wohlen.klassenkasse.network.handler.PacketDataRequestHandler;
import ch.kanti_wohlen.klassenkasse.network.handler.PacketDisconnectHandler;
import ch.kanti_wohlen.klassenkasse.network.handler.PacketHandshakeHandler;
import ch.kanti_wohlen.klassenkasse.network.handler.PacketLoginHandler;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketHandshake;
import ch.kanti_wohlen.klassenkasse.server.Server;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.ssl.SslHandler;

@Sharable
public class ServerChannelHandler extends ChannelInboundHandlerAdapter {

	private static final Logger LOGGER = Logger.getLogger(ServerChannelHandler.class.getSimpleName());

	private final SSLEngine sslEngine;
	private DatabaseHost host;

	public ServerChannelHandler(@NonNull ServerSocket socket) {
		if (Server.INSTANCE.getConfiguration().getBoolean("network.useSSL")) {
			SSLContext context;
			try {
				context = SSLContext.getDefault();
			} catch (NoSuchAlgorithmException e) {
				LOGGER.log(Level.SEVERE, "Could not set up SSL context. Set network.useSSL to false", e);
				sslEngine = null;
				return;
			}

			sslEngine = context.createSSLEngine(socket.getHostAddress().getHostAddress(), socket.getPort());
		} else {
			sslEngine = null;
		}
	}

	public boolean useSSL() {
		return sslEngine != null;
	}

	@SuppressWarnings("null")
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		LOGGER.finer("Channel registered.");

		ChannelPipeline pl = ctx.pipeline();
		pl.remove(this);

		String name = ctx.channel().remoteAddress().toString();
		host = Server.INSTANCE.newHost(name);

		// Packet en-/decoder
		pl.addLast("Packet Decoder", new PacketDecoder(host));
		pl.addLast("Packet Encoder", new PacketEncoder());

		// Add handshake handler
		pl.addLast("Handshake Handler", new PacketHandshakeHandler(this));

		pl.addLast("Error Handler", new ErrorHandler());
		super.channelRegistered(ctx);

		ctx.writeAndFlush(new PacketHandshake(Protocol.VERSION, useSSL()));
	}

	@SuppressWarnings("null")
	public void addPacketHandlers(ChannelHandlerContext ctx, boolean useSSL) {
		LOGGER.finer("Transforming pipeline to working mode.");

		ChannelPipeline pl = ctx.pipeline();

		if (useSSL && useSSL()) {
			SslHandler sslHandler = new SslHandler(sslEngine);
			pl.addFirst(sslHandler);
			LOGGER.fine("Using SSL for connection.");
		} else {
			LOGGER.fine("Not using SSL for connection, due to " + (!useSSL() ? "server" : "client") + " side.");
		}

		pl.remove(ErrorHandler.class);

		// General packet handler
		pl.addLast("Login Handler", new LoginHandler(host));

		// Handlers for the specific packets.
		pl.addLast("PacketLogin Handler", new PacketLoginHandler(host));
		pl.addLast("PacketAcctionCommitted Handler", new PacketActionCommittedHandler(host));
		pl.addLast("PacketDataRequest Handler", new PacketDataRequestHandler(host));
		pl.addLast("PacketClassVariables Handler", new PacketClassVariablesHandler(host));
		pl.addLast("PacketDisconnectHandler", new PacketDisconnectHandler());

		pl.addLast(new ErrorHandler());
	}
}
