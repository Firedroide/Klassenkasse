package ch.kanti_wohlen.klassenkasse.network.handler;

import java.util.logging.Logger;

import ch.kanti_wohlen.klassenkasse.network.Protocol;
import ch.kanti_wohlen.klassenkasse.network.ServerChannelHandler;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketHandshake;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PacketHandshakeHandler extends SimpleChannelInboundHandler<PacketHandshake> {

	private static final Logger LOGGER = Logger.getLogger(PacketHandshakeHandler.class.getSimpleName());
	private final ServerChannelHandler socket;

	public PacketHandshakeHandler(ServerChannelHandler socket) {
		this.socket = socket;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PacketHandshake msg) throws Exception {
		LOGGER.info("Got handshake packet from: " + ctx.channel().remoteAddress());

		boolean useSSL = msg.useSSL() && socket.useSSL();
		ctx.writeAndFlush(new PacketHandshake(Protocol.VERSION, useSSL));

		if (msg.getProtocolVersion() != Protocol.VERSION) {
			LOGGER.info("Protocol mismatch: Expected verison " + Protocol.VERSION + ", got " + msg.getProtocolVersion());
			ctx.close();
			return;
		}

		ctx.pipeline().remove(this);
		socket.addPacketHandlers(ctx, useSSL);
	}
}
