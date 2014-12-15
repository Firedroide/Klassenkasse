package ch.kanti_wohlen.klassenkasse.network.handler;

import java.util.Map;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketAccepted;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketClassVariables;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PacketClassVariablesHandler extends SimpleChannelInboundHandler<PacketClassVariables> {

	private final Host host;

	public PacketClassVariablesHandler(Host host) {
		this.host = host;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PacketClassVariables msg) {
		int classId = msg.getClassId();
		Map<String, String> variables = msg.getVariables();
		if (variables == null) {
			throw new IllegalArgumentException("Class variables map was null.");
		}

		host.updatePrintingVariablesForClass(classId, variables);

		ctx.writeAndFlush(new PacketAccepted());
	}
}
