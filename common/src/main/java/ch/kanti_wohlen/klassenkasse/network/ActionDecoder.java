package ch.kanti_wohlen.klassenkasse.network;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.ActionCreationException;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.login.UserLogin;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketActionCommitted;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ActionDecoder extends SimpleChannelInboundHandler<PacketActionCommitted> {

	private final @NonNull Host host;
	private final @NonNull IdMapper idMapper;
	private final UserLogin userLogin; // TODO: Set to NonNull and use

	public ActionDecoder(@NonNull Host host, UserLogin userLogin) {
		this.host = host;
		idMapper = host.getNewIdMapper();
		this.userLogin = userLogin;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PacketActionCommitted msg) throws Exception {
		ByteBuf buf = msg.getBuffer();
		byte controlByte;

		do {
			byte actionType = buf.readByte();
			short dataLength = buf.readShort();
			long clientActionId = buf.readLong();

			// Check the integrity of the data length
			if (dataLength < 0) {
				throw new ActionCreationException("Length of data was lower than 0 (" + dataLength + ").");
			} else if (buf.readableBytes() < dataLength + 1) {
				throw new ActionCreationException("Length of data was greater than the amount of data available.");
			}

			// Read data and create action
			@SuppressWarnings("null")
			@NonNull ByteBuf slice = buf.readSlice(dataLength);
			Action action = getActionByType(slice, actionType);
			idMapper.mapAction(clientActionId, action.getLocalId());

			// Check if all the data has been read
			if (slice.readableBytes() != 0) {
				throw new ActionCreationException("Action creation did not consume all data.");
			}

			// Check the control byte and then decide what to do
			controlByte = buf.readByte();
			if (controlByte != (byte) -2 && controlByte != (byte) -3) {
				// Can't trust the integrity of the other actions
				throw new PacketCreationException("Action control byte did not match.");
			}

			// Handle action
			User creator = null;
			if (userLogin != null) creator = userLogin.getLoggedInUser();

			host.addAction(action, creator);
		} while (controlByte != -2);
	}

	@NonNullByDefault
	private Action getActionByType(ByteBuf buf, byte actionType) throws ActionCreationException {
		Class<? extends Action> actionClass = Protocol.getActionClassById(actionType);
		if (actionClass == null) {
			throw new ActionCreationException("Unknown action type (" + actionType + ").");
		}

		Action resultAction = null;
		try {
			Constructor<? extends Action> actionConstructor = actionClass.getConstructor(Host.class);
			resultAction = actionConstructor.newInstance(host);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			// Unit-Tested, won't happen.
			e.printStackTrace();
		}
		if (resultAction == null) {
			throw new IllegalStateException("Action constructor returned a null value.");
		}

		resultAction.readData(buf, host, idMapper);

		return resultAction;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO
		ctx.close();
		super.exceptionCaught(ctx, cause);
	}
}
