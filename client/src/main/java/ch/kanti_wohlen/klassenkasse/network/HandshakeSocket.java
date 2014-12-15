package ch.kanti_wohlen.klassenkasse.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.oio.OioSocketChannel;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JLabel;

import ch.kanti_wohlen.klassenkasse.network.packet.PacketDisconnect;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketHandshake;
import ch.kanti_wohlen.klassenkasse.ui.util.IconProvider;

public class HandshakeSocket extends SimpleChannelInboundHandler<PacketHandshake> implements Runnable {

	private final JLabel label;
	private final String address;
	private final int port;

	private Channel channel;
	private boolean cancelled = false; // Fallback for strange circumstances

	public HandshakeSocket(JLabel resultLabel, String currentAddress, int currentPort) {
		label = resultLabel;
		address = currentAddress;
		port = currentPort;
	}

	@Override
	public void run() {
		if (address == null) return;
		if (port < 1) {
			return;
		}

		// Fail fast(er)
		try {
			InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			if (cancelled) return;
			label.setIcon(IconProvider.ERROR);
			label.setText("Die IP / Hostaddresse ist ungültig.");
			return;
		}

		if (cancelled) return;
		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(new OioEventLoopGroup(1));
			bootstrap.channel(OioSocketChannel.class);
			bootstrap.remoteAddress(address, port);
			bootstrap.handler(new ChannelInitializer<Channel>() {

				@Override
				protected void initChannel(Channel channel) throws Exception {
					// Packet de-/encoder
					channel.pipeline().addLast(new PacketDecoder(null));
					channel.pipeline().addLast(new PacketEncoder());

					// Handshake handler
					channel.pipeline().addLast(HandshakeSocket.this);
				}
			});

			if (cancelled) return;
			ChannelFuture f = bootstrap.connect().syncUninterruptibly();
			channel = f.channel();

			// Send the server a handshake packet
			channel.writeAndFlush(new PacketHandshake(Protocol.VERSION, true));

			// Wait until the connection has been terminated
			if (cancelled) return;
			channel.closeFuture().syncUninterruptibly();
		} catch (Exception e) {
			if (cancelled) return;
			if (e instanceof SocketException) {
				// We need to do this as the exception is not actually thrown
				label.setIcon(IconProvider.ERROR);
				label.setText("Es konnte keine Verbindung zum Server aufgebaut werden.");
			} else {
				label.setIcon(IconProvider.WARNING);
				label.setText("Es ist ein unbekannter Fehler aufgetreten.");
				e.printStackTrace();
			}
		}
	}

	public void cancel() {
		if (channel != null) {
			channel.writeAndFlush(new PacketDisconnect());
			channel.closeFuture().cancel(false);
		}
		cancelled = true;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PacketHandshake msg) throws Exception {
		if (msg.getProtocolVersion() == Protocol.VERSION) {
			label.setIcon(IconProvider.OK);
			label.setText("Gültiger Server");
		} else if (msg.getProtocolVersion() > Protocol.VERSION) {
			label.setIcon(IconProvider.ERROR);
			label.setText("Client nicht up-to-date.");
		} else {
			label.setIcon(IconProvider.ERROR);
			label.setText("Server nicht up-to-date. Kontaktieren Sie den Administrator.");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cancelled) return;
		if (cause instanceof ConnectTimeoutException) {
			label.setIcon(IconProvider.ERROR);
			label.setText("Der Server hat nicht auf eine Anfrage reagiert.");
		} else {
			label.setIcon(IconProvider.WARNING);
			label.setText("Es ist ein unbekannter Fehler aufgetreten.");
			cause.printStackTrace();
		}
	}
}
