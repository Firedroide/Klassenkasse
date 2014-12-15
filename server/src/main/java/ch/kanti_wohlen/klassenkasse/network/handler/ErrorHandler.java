package ch.kanti_wohlen.klassenkasse.network.handler;

import java.util.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ErrorHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger LOGGER = Logger.getLogger(ErrorHandler.class.getSimpleName());

	public ErrorHandler() {
		super(true);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		String packetType = msg == null ? "null" : msg.getClass().getSimpleName();
		LOGGER.warning("A packet of type " + packetType + " was not handled by any handler.");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
