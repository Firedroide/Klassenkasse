package ch.kanti_wohlen.klassenkasse.network.packet;

import io.netty.buffer.ByteBuf;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketPayments extends Packet {

	private Map<Integer, Payment> payments;

	public PacketPayments() {
		payments = Collections.emptyMap();
	}

	public PacketPayments(Map<Integer, Payment> payments) {
		this.payments = Collections.unmodifiableMap(new HashMap<>(payments));
	}

	public Map<Integer, Payment> getPayments() {
		return payments;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		Map<Integer, Payment> resultMap = new HashMap<>();
		while (buf.isReadable()) {
			int paymentId = buf.readInt();
			Date paymentDate = new Date(buf.readLong());
			String description = BufferUtil.readString(buf);
			MonetaryValue value = new MonetaryValue(buf.readLong());
			MonetaryValue rounding = new MonetaryValue(buf.readLong());

			resultMap.put(paymentId, new Payment(paymentId, paymentDate, description, value, rounding));
		}

		payments = Collections.unmodifiableMap(resultMap);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (Payment payment : payments.values()) {
			if (payment == null) continue;

			buf.writeInt(payment.getLocalId());
			buf.writeLong(payment.getDate().getTime());
			BufferUtil.writeString(buf, payment.getDescription());
			buf.writeLong(payment.getValue().getCentValue());
			buf.writeLong(payment.getRoundingValue().getCentValue());
		}
	}
}
