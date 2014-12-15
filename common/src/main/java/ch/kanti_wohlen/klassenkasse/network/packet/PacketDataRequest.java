package ch.kanti_wohlen.klassenkasse.network.packet;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.ActionSearchQuery;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(Way.CLIENT_TO_SERVER)
public class PacketDataRequest extends Packet {

	@NonNullByDefault
	public enum RequestType {
		// Void
		STUDENT_CLASSES,
		USERS,
		PAYMENTS,
		ROLES,
		USERNAMES,
		LOGGED_IN_USER,
		PRINTING_INFORMATION,

		// Integer
		STUDENT_CLASS_BY_ID(Integer.class),
		CLASS_VARIABLES_BY_ID(Integer.class),
		USER_BY_ID(Integer.class),
		USERS_BY_STUDENT_CLASS(Integer.class),
		USERS_WITH_PAYMENT(Integer.class),
		PAYMENT_BY_ID(Integer.class),
		PAYMENTS_BY_USER(Integer.class),
		ROLE_BY_ID(Integer.class),

		// Long
		ACTION_BY_ID(Long.class),

		// String
		USER_BY_USERNAME(String.class),

		// ActionSearchQuery
		SEARCH_ACTIONS(ActionSearchQuery.class);

		private final Class<? extends Object> argumentClass;

		private RequestType() {
			argumentClass = Void.class;
		}

		RequestType(Class<? extends Object> argumentClass) {
			this.argumentClass = argumentClass;
		}

		public Class<? extends Object> getArgumentClass() {
			return argumentClass;
		}
	}

	private RequestType requestType;
	private Object argument;

	public PacketDataRequest() {}

	@NonNullByDefault
	public PacketDataRequest(RequestType dataType) {
		if (!dataType.getArgumentClass().equals(Void.class)) {
			throw new IllegalArgumentException("dataType");
		}

		this.requestType = dataType;
	}

	@NonNullByDefault
	public PacketDataRequest(RequestType dataType, Object argument) {
		if (!dataType.getArgumentClass().isInstance(argument)) {
			throw new IllegalArgumentException("argument dataType");
		}

		this.requestType = dataType;
		this.argument = argument;
	}

	public RequestType getRequestedDataType() {
		return requestType;
	}

	public Object getArgument() {
		return argument;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		requestType = BufferUtil.readEnum(buf, RequestType.values());
		Class<? extends Object> requestClass = requestType.getArgumentClass();

		if (requestClass.equals(Void.class)) return;

		if (requestClass.equals(Integer.class)) {
			argument = buf.readInt();
		} else if (requestClass.equals(Long.class)) {
			argument = buf.readLong();
		} else if (requestClass.equals(String.class)) {
			argument = BufferUtil.readString(buf);
		} else if (requestClass.equals(ActionSearchQuery.class)) {
			ActionSearchQuery query = new ActionSearchQuery();
			query.read(buf, host);
			argument = query;
		} else {
			throw new IllegalArgumentException("Unhandled RequestType data type");
		}
	}

	@Override
	public void writeData(ByteBuf buf) {
		if (requestType == null) {
			throw new IllegalStateException("Request type for the request was not set.");
		}

		buf.writeByte(requestType.ordinal());

		Class<? extends Object> requestClass = requestType.getArgumentClass();
		if (!requestClass.equals(Void.class)) {
			if (argument == null) {
				throw new IllegalStateException("Request argument was null.");
			}

			if (requestClass.equals(Integer.class)) {
				buf.writeInt((int) argument);
			} else if (requestClass.equals(Long.class)) {
				buf.writeLong((long) argument);
			} else if (requestClass.equals(String.class)) {
				BufferUtil.writeString(buf, (String) argument);
			} else if (requestClass.equals(ActionSearchQuery.class)) {
				((ActionSearchQuery) argument).write(buf);
			} else {
				throw new IllegalArgumentException("Unhandled RequestType data type");
			}
		}
	}
}
