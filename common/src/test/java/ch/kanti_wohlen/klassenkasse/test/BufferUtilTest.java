package ch.kanti_wohlen.klassenkasse.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.junit.Assert;
import org.junit.Test;

import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

public class BufferUtilTest {

	@SuppressWarnings("null")
	@Test
	public void testNullCharactersAreRemoved() {
		ByteBuf buf = Unpooled.buffer();
		String stringWithNulls = "Hallo\0 \0Welt\0.";
		String stringWithoutNulls = "Hallo Welt.";

		BufferUtil.writeString(buf, stringWithNulls);

		buf.readerIndex(0);
		String roundTrip = BufferUtil.readString(buf);

		buf.release();

		Assert.assertEquals(stringWithoutNulls.length(), roundTrip.length());
		Assert.assertEquals(stringWithoutNulls, roundTrip);
	}
}
