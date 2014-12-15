package ch.kanti_wohlen.klassenkasse.network.packet;

import io.netty.buffer.ByteBuf;

import java.awt.image.BufferedImage;

import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType.Way;
import ch.kanti_wohlen.klassenkasse.util.BufferUtil;

@PacketType(Way.SERVER_TO_CLIENT)
public class PacketPrintingInformation extends Packet {

	private String footerNote;
	private BufferedImage headerImage;

	public PacketPrintingInformation() {
		footerNote = null;
		headerImage = null;
	}

	public PacketPrintingInformation(String footerNote, BufferedImage headerImage) {
		this.footerNote = footerNote;
		this.headerImage = headerImage;
	}

	public String getFooterNote() {
		return footerNote;
	}

	public void setFooterNote(String newFooterNote) {
		footerNote = newFooterNote;
	}

	public BufferedImage getHeaderImage() {
		return headerImage;
	}

	public void setHeaderImage(BufferedImage newHeaderImage) {
		this.headerImage = newHeaderImage;
	}

	@Override
	public void readData(ByteBuf buf, Host host) {
		footerNote = BufferUtil.readString(buf);
		headerImage = BufferUtil.readImage(buf);
	}

	@Override
	public void writeData(ByteBuf buf) {
		BufferUtil.writeString(buf, footerNote);
		BufferUtil.writeImage(buf, headerImage);
	}
}
