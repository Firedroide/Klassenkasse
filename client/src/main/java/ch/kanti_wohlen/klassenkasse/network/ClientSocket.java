package ch.kanti_wohlen.klassenkasse.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.action.BaseAction;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketActionCommitted;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketDisconnect;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketErrorEncountered;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketHandshake;
import ch.kanti_wohlen.klassenkasse.ui.dialog.ErrorEncounteredDialog;

// TODO: Error handling
@NonNullByDefault
public class ClientSocket implements Runnable {

	@SuppressWarnings("null")
	private static final Logger LOGGER = Logger.getLogger(ClientSocket.class.getSimpleName());

	private final String host;
	private final int port;
	private final EventLoopGroup eventGroup;
	private final Host dataHost;

	private final OutgoingSender sender;
	private final IncomingListener listener;

	private boolean stopping;
	private @Nullable Channel channel;

	public ClientSocket(String connectionHost, int connectionPort, Host dataHost) {
		this.dataHost = dataHost;
		host = connectionHost;
		port = connectionPort;
		eventGroup = new NioEventLoopGroup();

		sender = new OutgoingSender();
		listener = new IncomingListener(sender);
	}

	public void stop() {
		if (stopping) return;

		stopping = true;
		final Channel channel = this.channel;
		if (channel != null) {
			channel.writeAndFlush(new PacketDisconnect());
			channel.close();
		}
	}

	public void run() {
		try {
			// Create the bootstrap and set options
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(eventGroup);
			bootstrap.channel(NioSocketChannel.class);
			bootstrap.remoteAddress(host, port);
			bootstrap.handler(new ChannelInitializer<Channel>() {

				@Override
				protected void initChannel(@Nullable Channel channel) throws Exception {
					if (channel == null) return;

					// Packet de-/encoder
					channel.pipeline().addLast("Packet Decoder", new PacketDecoder(dataHost));
					channel.pipeline().addLast("Packet Encoder", new PacketEncoder());
				}
			});
			bootstrap.validate();

			// As long as we're not terminating the connection, try to reconnect
			while (!stopping) {
				// Start listening to responses from the server
				ChannelFuture f = bootstrap.connect().sync();
				Channel channel = f.channel();
				if (channel == null) return;
				this.channel = channel;

				// Send the server a handshake packet as soon as we're connected
				try {
					channel.writeAndFlush(new PacketHandshake(Protocol.VERSION, true)).awaitUninterruptibly();
				} catch (Exception e) {
					e.printStackTrace();

					channel.close();
					return;
				}

				listener.register(channel);
				sender.register(channel);

				// Wait until the connection has been terminated
				channel.closeFuture().sync();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			eventGroup.shutdownGracefully();
		}
	}

	public Future<? extends Packet> transmitPacket(Packet packet) {
		sender.outgoingQueue.add(packet);
		return listener.packetFuture(packet);
	}

	public Future<? extends Packet> transmitAction(BaseAction... actions) {
		PacketActionCommitted actionCommitted = new PacketActionCommitted(actions);
		return transmitPacket(actionCommitted);
	}

	private static class OutgoingSender implements Runnable {

		private final Queue<Object> outgoingQueue;
		private final Queue<Object> transmittedQueue;

		private @Nullable Channel channel;
		private @Nullable ScheduledFuture<?> future;

		public OutgoingSender() {
			outgoingQueue = new ConcurrentLinkedQueue<>();
			transmittedQueue = new ConcurrentLinkedQueue<>();
		}

		@Override
		public void run() {
			final Channel ch = channel;
			if (ch == null || !ch.isActive()) return;

			boolean written = false;
			for (Iterator<Object> i = outgoingQueue.iterator(); i.hasNext();) {
				Object outgoing = i.next();
				ch.write(outgoing);
				outgoingQueue.remove();
				transmittedQueue.add(outgoing);
				written = true;
			}

			if (written) {
				ch.flush();
			}
		}

		private void rewind(boolean retryLast) {
			if (channel != null) {
				throw new IllegalStateException("Must cancel sender before rewinding.");
			}

			// Copying using a LinkedList, as ConcurrentLinkedQueue doesn't support addFirst
			LinkedList<Object> elements = new LinkedList<>(outgoingQueue);
			Iterator<Object> i = transmittedQueue.iterator();
			if (!retryLast && i.hasNext()) {
				// Not retrying, drop the first item
				i.next();
			}

			while (i.hasNext()) {
				elements.addFirst(i.next());
			}

			outgoingQueue.clear();
			outgoingQueue.addAll(elements);
		}

		private void register(Channel channel) {
			cancel();
			this.channel = channel;
			future = channel.eventLoop().scheduleAtFixedRate(this, 10, 10, TimeUnit.MILLISECONDS);
		}

		private void cancel() {
			ScheduledFuture<?> future = this.future;
			if (future != null) {
				future.cancel(false);
				channel = null;
			}
		}
	}

	private static class IncomingListener extends MessageToMessageDecoder<Packet> {

		private final Queue<Object> transmittedQueue;
		private final Queue<PacketFuture> incomingQueue;
		private final OutgoingSender sender;
		private @Nullable ChannelPipeline pipeline;

		public IncomingListener(OutgoingSender sender) {
			transmittedQueue = new ConcurrentLinkedQueue<>();
			incomingQueue = new ConcurrentLinkedQueue<>();
			this.sender = sender;
		}

		public Future<? extends Packet> packetFuture(Object transmitted) {
			PacketFuture future = new PacketFuture();
			transmittedQueue.add(transmitted);
			incomingQueue.add(future);

			LOGGER.info("Sent out " + transmitted.getClass().getSimpleName() + ", waiting for response.");
			return future;
		}

		public void register(Channel channel) {
			ChannelPipeline pipeline = this.pipeline;
			if (pipeline != null) {
				pipeline.remove(this);
			}

			channel.pipeline().addAfter("Packet Decoder", "Packet Listener", this);
			this.pipeline = channel.pipeline();
		}

		@Override
		public void channelUnregistered(@Nullable ChannelHandlerContext ctx) throws Exception {
			for (PacketFuture future : incomingQueue) {
				future.error(new ChannelException("The channel was closed."));
			}
		}

		@Override
		protected void decode(@Nullable ChannelHandlerContext ctx, @Nullable Packet msg, @Nullable List<Object> out)
				throws Exception {
			if (msg == null || out == null) return;

			if (msg instanceof PacketErrorEncountered) {
				errorEncountered((PacketErrorEncountered) msg);
				return;
			}
			out.add(msg);

			if (msg instanceof PacketHandshake) return; // Discard handshakes

			Object lastTransmitted = sender.transmittedQueue.poll();
			Object lastRegistered = transmittedQueue.poll();

			if (lastTransmitted != lastRegistered) {
				throw new IllegalStateException("The server did not respond to a previous packet.");
			}

			LOGGER.info("Got " + msg.getClass().getSimpleName() + " as a response to "
					+ lastTransmitted.getClass().getSimpleName());

			PacketFuture future = incomingQueue.poll();
			if (future == null) {
				throw new IllegalStateException("The future was null, which should not be the case.");
			}

			future.set(msg);
		}

		private void errorEncountered(PacketErrorEncountered error) {
			Object queued = sender.transmittedQueue.remove();
			transmittedQueue.remove();
			if (!(queued instanceof Packet)) return;

			Packet packet = (Packet) queued;
			LOGGER.info("Received an error for a packet of type " + packet.getClass().getSimpleName());

			boolean retry = false;
			switch (error.getNetworkError()) {
			case INVALID_LOGIN:
				break;
			case UNKNOWN_ERROR:
				ErrorEncounteredDialog dialog = new ErrorEncounteredDialog(error.getReason());
				dialog.setVisible(true);
				retry = dialog.getResult() == ErrorEncounteredDialog.Result.RETRY;
				break;
			default:
				break;
			}

			PacketFuture future = incomingQueue.poll();
			if (future == null) {
				throw new IllegalStateException("The future was null, which should not be the case.");
			}
			future.error(new Exception()); // TODO, but works for now

			sender.cancel();
			sender.rewind(retry);
		}

		private static class PacketFuture implements Future<Packet> {

			private boolean done;
			private @Nullable Packet resultPacket = null;

			private boolean failed;
			private @Nullable Throwable throwable;

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public synchronized boolean isDone() {
				return done;
			}

			private synchronized void set(Packet packet) {
				if (done) throw new IllegalStateException("Already done.");

				resultPacket = packet;
				done = true;
				notifyAll();
			}

			private synchronized void error(Throwable throwable) {
				if (done) throw new IllegalStateException("Already done.");

				this.throwable = throwable;
				failed = true;
				done = true;
				notifyAll();
			}

			@Override
			public synchronized Packet get() throws InterruptedException, ExecutionException {
				while (!done) {
					wait();
				}

				return getOrFail();
			}

			@Override
			public synchronized Packet get(long timeout, @Nullable TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException {
				if (unit == null) throw new NullPointerException("unit");
				long millis = unit.toMillis(timeout);
				if (millis > Integer.MAX_VALUE || millis < 0) {
					throw new IllegalArgumentException("Timeout out of range");
				}

				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.MILLISECOND, (int) millis);

				while (!done && calendar.after(Calendar.getInstance())) {
					wait(millis);
				}

				if (!done && !calendar.after(Calendar.getInstance())) {
					throw new TimeoutException();
				}

				return getOrFail();
			}

			private Packet getOrFail() throws ExecutionException {
				if (failed) {
					throw new ExecutionException(throwable);
				}

				final Packet result = resultPacket;
				if (result != null) {
					return result;
				} else {
					throw new ExecutionException(new NullPointerException());
				}
			}
		}
	}
}
