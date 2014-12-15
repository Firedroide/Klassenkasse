package ch.kanti_wohlen.klassenkasse.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

public class ServerSocket implements Runnable {

	private final @NonNull EventLoopGroup bossGroup;
	private final @NonNull EventLoopGroup workerGroup;
	private final @NonNull InetAddress host;
	private final int port;

	@NonNullByDefault
	public ServerSocket(InetAddress connectionHost, int connectionPort) {
		host = connectionHost;
		port = connectionPort;
		bossGroup = new NioEventLoopGroup(1);
		workerGroup = new NioEventLoopGroup();
	}

	@Override
	public void run() {
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
			bootstrap.childHandler(new ServerChannelHandler(this));

			ChannelFuture f = bootstrap.bind(host, port).syncUninterruptibly();
			f.channel().closeFuture().syncUninterruptibly();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public @NonNull InetAddress getHostAddress() {
		return host;
	}

	public int getPort() {
		return port;
	}
}
