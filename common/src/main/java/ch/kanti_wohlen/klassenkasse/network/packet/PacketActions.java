package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.action.ActionCreationException;
import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.PacketCreationException;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import io.netty.buffer.ByteBuf;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketActions extends Packet {

	private static final int OVERHEAD = 24;

	private Map<Long, BaseAction> actions;

	public PacketActions() {
		actions = Collections.emptyMap();
	}

	public PacketActions(Map<Long, BaseAction> actions) {
		this.actions = Collections.unmodifiableMap(new HashMap<>(actions));
	}

	public Map<Long, BaseAction> getActions() {
		return actions;
	}

	@Override
	public void readData(ByteBuf buf, Host host) throws PacketCreationException {
		Map<Long, BaseAction> resultMap = new HashMap<>();
		while (buf.isReadable(OVERHEAD)) {
			byte actionType = buf.readByte();
			short dataLength = buf.readShort();
			long actionId = buf.readLong();
			int creatorId = buf.readInt();
			Date creationDate = new Date(buf.readLong());
			boolean applied = buf.readBoolean();

			// Check the integrity of the data length
			if (dataLength < 0) {
				throw new PacketCreationException("Action length of data was lower than 0 (" + dataLength + ").");
			} else if (buf.readableBytes() < dataLength + 1) {
				buf.readerIndex(buf.readerIndex() - OVERHEAD);
				break;
			}

			// Read data and create action
			@SuppressWarnings("null")
			@NonNull
			ByteBuf slice = buf.readSlice(dataLength);
			Action action;
			try {
				action = Action.createActionByType(host, actionType, slice);
			} catch (ActionCreationException e) {
				throw new PacketCreationException(e);
			}

			// Check if all the data has been read
			if (slice.readableBytes() != 0) {
				throw new PacketCreationException("Action creation did not consume all data.");
			}

			// Check the control byte and then decide what to do
			byte controlByte = buf.readByte();
			if (controlByte != (byte) -2) {
				// Can't trust the integrity of the other actions
				throw new PacketCreationException("Action control byte did not match.");
			}

			BaseAction base = new BaseAction(action, actionId, creatorId, creationDate, applied);
			resultMap.put(actionId, base);
		}

		actions = Collections.unmodifiableMap(resultMap);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (BaseAction base : actions.values()) {
			if (base == null) continue;

			// Get the action conveyed by this BaseAction
			Action action = base.getAction();

			// Action header: Action type ID and length placeholder
			buf.writeByte(action.getActionId());
			int lengthIndex = buf.writerIndex();
			buf.writeShort(0);

			// Write BaseAction data
			buf.writeLong(base.getLocalId());
			buf.writeInt(base.getCreatorId());
			buf.writeLong(base.getCreationDate().getTime());
			buf.writeBoolean(base.isApplied());

			// Write custom Action data
			int startIndex = buf.writerIndex();
			action.writeData(buf);

			// Set length before data
			int dataLength = buf.writerIndex() - startIndex;
			if (dataLength > Short.MAX_VALUE) {
				throw new IllegalStateException("Too much data for one action (" + dataLength + " > 32767)");
			}
			buf.setShort(lengthIndex, dataLength);

			// Action data termination byte
			buf.writeByte(-2);
		}
	}
}
