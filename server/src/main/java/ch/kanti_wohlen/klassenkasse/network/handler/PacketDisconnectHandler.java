package ch.kanti_wohlen.klassenkasse.network.handler;

import java.util.logging.Logger;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketDisconnect;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handles {@linkplain PacketDisconnectHandler disconnect packets} sent by the client.
 * <p>
 * Simply calls {@link ChannelHandlerContext#close()} to terminate the connection.
 * </p>
 */
public class PacketDisconnectHandler extends SimpleChannelInboundHandler<PacketDisconnect> {

	private static final Logger LOGGER = Logger.getLogger(PacketDisconnectHandler.class.getSimpleName());

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PacketDisconnect msg) throws Exception {
		ctx.close();
		LOGGER.fine("Terminated client connection");
	}
}
