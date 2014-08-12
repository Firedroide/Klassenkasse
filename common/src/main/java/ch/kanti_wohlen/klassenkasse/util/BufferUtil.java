package ch.kanti_wohlen.klassenkasse.util;

import org.eclipse.jdt.annotation.NonNull;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

// TODO: Change exception, clean up, document
public final class BufferUtil {

	private BufferUtil() {}

	public static void writeString(@NonNull ByteBuf buf, String str) {
		if (str != null) {
			buf.writeBytes(str.getBytes(CharsetUtil.UTF_8));
		}
		buf.writeZero(1);
	}

	public static @NonNull String readString(@NonNull ByteBuf buf) {
		// Get length of string
		int strLen = buf.bytesBefore((byte) 0);
		if (strLen < 0) {
			throw new IllegalArgumentException("No String terminator in buffer found.");
		}

		// Read string into byte array
		byte[] b = new byte[strLen];
		buf.readBytes(b, 0, strLen);

		// Skip NULL termination character
		buf.skipBytes(1);

		// Return the decoded string
		return new String(b, CharsetUtil.UTF_8);
	}

	public static <T> T readEnum(ByteBuf buf, T[] values) throws IllegalArgumentException {
		byte b = buf.readByte();
		if (b < 0 || b > values.length) {
			throw new IllegalArgumentException("Enum ordinal value out of bounds (" + b + ").");
		}
		return values[b];
	}
}
