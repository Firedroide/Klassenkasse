package ch.kanti_wohlen.klassenkasse.network.packet;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketPayments extends Packet {

	private Collection<Payment> payments;

	public PacketPayments() {
		payments = Collections.emptyList();
	}

	public PacketPayments(Payment... payments) {
		this.payments = Arrays.asList(payments);
	}

	public PacketPayments(Collection<Payment> payments) {
		this.payments = new ArrayList<>(payments);
	}

	public Collection<Payment> getPayments() {
		return Collections.unmodifiableCollection(payments);
	}

	@Override
	public void readData(ByteBuf buf) {
		List<Payment> resultList = new ArrayList<>();
		while (buf.isReadable()) {
			int paymentId = buf.readInt();
			Date paymentDate = new Date(buf.readLong());
			String description = BufferUtil.readString(buf);
			MonetaryValue value = new MonetaryValue(buf.readLong());
			resultList.add(new Payment(paymentId, paymentDate, description, value));
		}
		payments = Collections.unmodifiableList(resultList);
	}

	@Override
	public void writeData(ByteBuf buf) {
		for (Payment payment : payments) {
			buf.writeInt(payment.getLocalId());
			buf.writeLong(payment.getPaymentDate().getTime());
			BufferUtil.writeString(buf, payment.getPaymentDescription());
			buf.writeLong(payment.getMonetaryValue().getCentValue());
		}
	}
}
