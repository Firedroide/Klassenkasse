package ch.kanti_wohlen.klassenkasse.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketAccepted;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketActionCommitted;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketErrorEncountered;

public class PacketActionCommittedHandler extends SimpleChannelInboundHandler<PacketActionCommitted> {

	private static final Logger LOGGER = Logger.getLogger(PacketActionCommittedHandler.class.getSimpleName());

	private final Host host;

	public PacketActionCommittedHandler(Host host) {
		this.host = host;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PacketActionCommitted msg) throws Exception {
		BaseAction[] actions = msg.getActions().toArray(new BaseAction[0]);

		StringBuilder sb = new StringBuilder("Applying ").append(actions.length).append(" action(s), of type ");
		for (BaseAction action : actions) {
			sb.append(action.getAction().getClass().getSimpleName()).append(", ");
		}
		LOGGER.info(sb.toString());

		try {
			host.addActions(actions);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Applying of actions failed!", e);

			ctx.writeAndFlush(new PacketErrorEncountered());
			return;
		}

		ctx.writeAndFlush(new PacketAccepted());
	}
}
