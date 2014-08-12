package ch.kanti_wohlen.klassenkasse.network.packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import io.netty.buffer.ByteBuf;

@PacketType(Way.CLIENT_TO_SERVER)
public class PacketDataRequest extends Packet {

	public static enum RequestType {
		STUDENT_CLASS_BY_ID,
		USER_BY_ID,
		USER_BY_STUDENT_CLASS,
		PAYMENT_BY_ID,
		PAYMENT_BY_USER,
		E_MAIL_ADDRESSES
	}

	private RequestType requestType;
	private List<Integer> dataIds;

	public PacketDataRequest() {
		requestType = null;
		dataIds = Collections.emptyList();
	}

	public PacketDataRequest(RequestType dataType, Integer... dataIds) {
		this.requestType = dataType;
		this.dataIds = Arrays.asList(dataIds);
	}

	public RequestType getRequestedDataType() {
		return requestType;
	}

	public List<Integer> getRequestedDataIds() {
		return dataIds;
	}

	@Override
	public void readData(ByteBuf buf) {
		requestType = BufferUtil.readEnum(buf, RequestType.values());
		List<Integer> resultIds = new ArrayList<>(buf.readableBytes() / 4);
		while (buf.isReadable(4)) {
			resultIds.add(buf.readInt());
		}
		dataIds = Collections.unmodifiableList(resultIds);
	}

	@Override
	public void writeData(ByteBuf buf) {
		if (requestType == null ) {
			throw new IllegalStateException("Request type for the request was not set.");
		} else if (dataIds == null) {
			throw new IllegalStateException("Requested data IDs was null or empty.");
		}

		buf.writeByte(requestType.ordinal());
		for (Integer i : dataIds) {
			buf.writeInt(i);
		}
	}
}
