package ch.kanti_wohlen.klassenkasse.test;

import static org.junit.Assert.fail;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.reflections.Reflections;

import ch.kanti_wohlen.klassenkasse.network.Protocol;
import ch.kanti_wohlen.klassenkasse.network.packet.Packet;
import ch.kanti_wohlen.klassenkasse.network.packet.PacketType;

public class PacketTest {

	private List<String> issues = new ArrayList<>();

	private static Set<Class<? extends Packet>> getAvailablePacketClasses() {
		Reflections ref = new Reflections(Packet.class.getPackage().getName());
		Set<Class<? extends Packet>> subClasses = ref.getSubTypesOf(Packet.class);
		Iterator<Class<? extends Packet>> iterator = subClasses.iterator();
		while (iterator.hasNext()) {
			Class<? extends Packet> clazz = iterator.next();
			if (Modifier.isAbstract(clazz.getModifiers())) {
				iterator.remove();
			}
		}
		return subClasses;
	}

	@Test
	public void testAllPacketsRegistered() {
		Set<Class<? extends Packet>> packetClasses = getAvailablePacketClasses();

		System.out.println("Testing IDs of " + packetClasses.size() + " packets.");

		for (Class<? extends Packet> packet : packetClasses) {
			try {
				// Get the packet ID by its class
				// Throws a NullPointerException if the class was not registered
				Protocol.getPacketId(packet);
			} catch (NullPointerException e) {
				issues.add("Packet " + packet.getSimpleName() + " had no ID assinged in Protocol.");
			}
		}
	}

	@Test
	public void testAllPacketsHaveWayAnnotation() {
		Set<Class<? extends Packet>> packetClasses = getAvailablePacketClasses();

		for (Class<? extends Packet> packet : packetClasses) {
			if (!packet.isAnnotationPresent(PacketType.class)) {
				issues.add("Packet " + packet.getSimpleName() + " had no PacketType annotation defined.");
			}
		}
	}

	@Test
	public void testAllPacketsHaveNoArgConstructor() {
		for (Class<? extends Packet> packetClass : getAvailablePacketClasses()) {
			try {
				// Constructs a new instance of the packet
				packetClass.newInstance();
			} catch (IllegalAccessException e) {
				issues.add("Packet " + packetClass.getSimpleName() + " did not have a no-args constructor defined.");
			} catch (InstantiationException e) {
				fail("Packet " + packetClass.getSimpleName() + " could not be instantiated (abstract class ?).");
				e.printStackTrace();
			} catch (ExceptionInInitializerError e) {
				fail("Packet " + packetClass.getSimpleName() + " threw an exception in its no-args constructor.");
				e.printStackTrace();
			}
		}
	}

	@After
	public void failOnIssues() {
		if (!issues.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String issue : issues) {
				sb.append(issue).append("\n");
			}
			sb.setLength(sb.length() - 1);

			fail(sb.toString());
		}
	}
}
