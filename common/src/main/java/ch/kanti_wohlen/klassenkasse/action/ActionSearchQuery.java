package ch.kanti_wohlen.klassenkasse.action;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.actions.ActionActions;
import ch.kanti_wohlen.klassenkasse.action.classes.ActionClass;
import ch.kanti_wohlen.klassenkasse.action.paymentUsers.ActionPaymentUsers;
import ch.kanti_wohlen.klassenkasse.action.payments.ActionPayment;
import ch.kanti_wohlen.klassenkasse.action.users.ActionUser;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;
import ch.kanti_wohlen.klassenkasse.framework.id.IdMapper;
import ch.kanti_wohlen.klassenkasse.network.Protocol;
import ch.kanti_wohlen.klassenkasse.util.BiMap;

@NonNullByDefault
public class ActionSearchQuery implements Cloneable {

	private static final BiMap<Byte, Class<? extends Action>> ABSTRACT_CLASSES = new BiMap<>();
	static {
		ABSTRACT_CLASSES.put((byte) -1, ActionClass.class);
		ABSTRACT_CLASSES.put((byte) -2, ActionUser.class);
		ABSTRACT_CLASSES.put((byte) -3, ActionPayment.class);
		ABSTRACT_CLASSES.put((byte) -4, ActionPaymentUsers.class);
		ABSTRACT_CLASSES.put((byte) -5, ActionActions.class);
	}

	private long offset;
	private short limit;

	private @Nullable Class<? extends Action> actionType;
	private @Nullable Integer creatorId;
	private @Nullable Date before;
	private @Nullable Date after;
	private @Nullable Boolean applied;

	private @Nullable Integer classId;
	private @Nullable Integer userId;
	private @Nullable Integer paymentId;

	public ActionSearchQuery() {
		offset = 0;
		limit = 20;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public short getLimit() {
		return limit;
	}

	public void setLimit(short limit) {
		this.limit = limit;
	}

	public @Nullable Class<? extends Action> getActionType() {
		return actionType;
	}

	public void setActionType(@Nullable Class<? extends Action> actionType) {
		this.actionType = actionType;
	}

	public @Nullable Integer getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(@Nullable Integer creatorId) {
		this.creatorId = creatorId;
	}

	public void setCreator(@Nullable User user) {
		this.creatorId = (user == null) ? null : user.getLocalId();
	}

	public @Nullable Date getBefore() {
		return before;
	}

	public void setBefore(@Nullable Date before) {
		this.before = before;
	}

	public @Nullable Date getAfter() {
		return after;
	}

	public void setAfter(@Nullable Date after) {
		this.after = after;
	}

	public @Nullable Boolean getApplied() {
		return applied;
	}

	public void setApplied(@Nullable Boolean applied) {
		this.applied = applied;
	}

	public @Nullable Integer getClassId() {
		return classId;
	}

	public void setClass(@Nullable StudentClass studentClass) {
		this.classId = (studentClass == null) ? null : studentClass.getLocalId();
	}

	public void setClassId(@Nullable Integer classId) {
		this.classId = classId;
	}

	public @Nullable Integer getUserId() {
		return userId;
	}

	public void setUser(@Nullable User user) {
		this.userId = (user == null) ? null : user.getLocalId();
	}

	public void setUserId(@Nullable Integer userId) {
		this.userId = userId;
	}

	public @Nullable Integer getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(@Nullable Integer paymentId) {
		this.paymentId = paymentId;
	}

	public void write(ByteBuf buf) {
		// Non-null fields
		buf.writeLong(offset);
		buf.writeShort(limit);

		// Nullable fields
		Class<? extends Action> actionClass = actionType;
		if (actionClass != null && !actionClass.equals(Action.class)) {
			if (Modifier.isAbstract(actionClass.getModifiers())) {
				buf.writeByte(ABSTRACT_CLASSES.inverse().get(actionClass));
			} else {
				buf.writeByte(Protocol.getActionId(actionType));
			}
		} else {
			buf.writeByte(Byte.MIN_VALUE);
		}

		if (creatorId != null) {
			buf.writeInt(creatorId);
		} else {
			buf.writeInt(Integer.MIN_VALUE);
		}

		if (before != null) {
			buf.writeLong(before.getTime());
		} else {
			buf.writeLong(Long.MIN_VALUE);
		}

		if (after != null) {
			buf.writeLong(after.getTime());
		} else {
			buf.writeLong(Long.MIN_VALUE);
		}

		if (applied != null) {
			buf.writeBoolean(applied.booleanValue());
		} else {
			buf.writeByte(0b10101010);
		}

		if (classId != null) {
			buf.writeInt(classId);
		} else {
			buf.writeInt(Integer.MIN_VALUE);
		}

		if (userId != null) {
			buf.writeInt(userId);
		} else {
			buf.writeInt(Integer.MIN_VALUE);
		}

		if (paymentId != null) {
			buf.writeInt(paymentId);
		} else {
			buf.writeInt(Integer.MIN_VALUE);
		}
	}

	public void read(ByteBuf buf, Host host) {
		IdMapper idMapper = host.getIdMapper();

		// Non-null fields
		offset = buf.readLong();
		limit = buf.readShort();

		// Nullable fields
		byte actionId = buf.readByte();
		if (actionId >= 0) {
			actionType = Protocol.getActionClassById(actionId);
		} else {
			actionType = ABSTRACT_CLASSES.get(actionId);
		}

		int creatorId = buf.readInt();
		this.creatorId = (creatorId == Integer.MIN_VALUE) ? null : idMapper.getUserMapping(creatorId);

		long before = buf.readLong();
		this.before = (before == Long.MIN_VALUE) ? null : new Date(before);

		long after = buf.readLong();
		this.after = (after == Long.MIN_VALUE) ? null : new Date(after);

		byte applied = buf.readByte();
		if (applied == 1) {
			this.applied = Boolean.TRUE;
		} else if (applied == 0) {
			this.applied = Boolean.FALSE;
		} else {
			this.applied = null;
		}

		int classId = buf.readInt();
		this.classId = (classId == Integer.MIN_VALUE) ? null : idMapper.getClassMapping(classId);

		int userId = buf.readInt();
		this.userId = (userId == Integer.MIN_VALUE) ? null : idMapper.getUserMapping(userId);

		int paymentId = buf.readInt();
		this.paymentId = (paymentId == Integer.MIN_VALUE) ? null : idMapper.getPaymentMapping(paymentId);
	}

	@Override
	public ActionSearchQuery clone() {
		ActionSearchQuery clone = new ActionSearchQuery();
		clone.offset = offset;
		clone.limit = limit;

		clone.actionType = actionType;
		clone.creatorId = creatorId;
		clone.before = before;
		clone.after = after;
		clone.applied = applied;

		clone.classId = classId;
		clone.paymentId = paymentId;
		clone.userId = userId;

		return clone;
	}

	@SuppressWarnings("null")
	@Override
	public String toString() {
		try {
			StringBuilder out = new StringBuilder(ActionSearchQuery.class.getSimpleName());
			out.append("[");
			for (Field field : ActionSearchQuery.class.getDeclaredFields()) {
				Object value = field.get(this);
				if (value == null || value instanceof Map<?, ?>) continue;

				out.append(field.getName()).append("=");
				if (value instanceof Class<?>) {
					out.append(((Class<?>) value).getSimpleName());
				} else {
					out.append(value);
				}
				out.append(",");
			}
			out.setLength(out.length() - 1);
			return out.append("]").toString();
		} catch (Exception e) {
			return super.toString();
		}
	}
}
