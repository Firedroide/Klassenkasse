package ch.kanti_wohlen.klassenkasse.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public final class BufferUtil {

	private BufferUtil() {}

	public static <T> T readEnum(ByteBuf buf, T[] values) throws IllegalArgumentException {
		byte b = buf.readByte();
		if (b < 0 || b > values.length) {
			throw new IllegalArgumentException("Enum ordinal value out of bounds (" + b + ").");
		}
		return values[b];
	}

	public static void writeEnum(ByteBuf buf, Enum<?> enumValue) {
		buf.writeByte(enumValue.ordinal());
	}

	public static @NonNull String readString(@NonNull ByteBuf buf) {
		// Get length of string
		int strLen = buf.bytesBefore((byte) 0);
		if (strLen < 0) {
			throw new IllegalArgumentException("No String terminator found.");
		}

		// Read string into byte array
		byte[] b = new byte[strLen];
		buf.readBytes(b, 0, strLen);

		// Skip termination character
		buf.skipBytes(1);

		// Return the decoded string
		return new String(b, CharsetUtil.UTF_8);
	}

	public static void writeString(@NonNull ByteBuf buf, String str) {
		// Write char values, encoded in UTF-8
		if (str != null) {
			String noZeroChars = str.replace("\0", "");
			buf.writeBytes(noZeroChars.getBytes(CharsetUtil.UTF_8));
		}

		// Write termination character
		buf.writeZero(1);
	}

	public static @Nullable BufferedImage readImage(ByteBuf buf) {
		int dataLength = buf.readInt();
		if (dataLength == 0) return null;

		byte[] data = new byte[dataLength];
		buf.readBytes(data);
		byte control = buf.readByte();
		if (control != 0) throw new IllegalStateException("Control byte did not match!");

		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
			return img;
		} catch (IOException e) {
			throw new IllegalStateException("An error occurred during reading image", e);
		}
	}

	public static void writeImage(ByteBuf buf, BufferedImage img) {
		if (img == null) {
			buf.writeInt(0);
			return;
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			boolean written = ImageIO.write(img, "png", outputStream);
			if (!written) throw new IllegalStateException("Could not write a PNG image?");
		} catch (IOException e) {
			throw new IllegalStateException("An error occurred while writing an image", e);
		}

		byte[] data = outputStream.toByteArray();
		buf.writeInt(data.length);
		buf.writeBytes(data);
		buf.writeZero(1);
	}
}
