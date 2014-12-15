package ch.kanti_wohlen.klassenkasse.test;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.reflections.Reflections;

import ch.kanti_wohlen.klassenkasse.action.Action;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.network.Protocol;
import static org.junit.Assert.*;

public class ActionTest {

	private static Set<Class<? extends Action>> getAvailableActionClasses() {
		Reflections ref = new Reflections(Action.class.getPackage().getName());
		Set<Class<? extends Action>> subClasses = ref.getSubTypesOf(Action.class);
		Iterator<Class<? extends Action>> iterator = subClasses.iterator();
		while (iterator.hasNext()) {
			Class<? extends Action> clazz = iterator.next();
			if (Modifier.isAbstract(clazz.getModifiers())) {
				iterator.remove();
			}
		}
		return subClasses;
	}

	@Test
	public void testAllActionsRegistered() {
		Set<Class<? extends Action>> actionClasses = getAvailableActionClasses();

		System.out.println("Testing IDs of " + actionClasses.size() + " actions.");

		for (Class<? extends Action> action : actionClasses) {
			try {
				// Get the action ID by its class
				// Throws a NullPointerException if the class was not registered
				Protocol.getActionId(action);
			} catch (NullPointerException e) {
				fail("Action " + action.getSimpleName() + " had no ID assinged in Protocol.");
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testAllActionsHaveHostConstructor() {
		for (Class<? extends Action> actionClass : getAvailableActionClasses()) {
			try {
				// Gets the constructor for the action
				actionClass.getConstructor(Host.class, ByteBuf.class);
			} catch (NoSuchMethodException e) {
				fail("Action " + actionClass.getSimpleName() + " did not have a Host-only constructor defined.");
				e.printStackTrace();
			}
		}
	}
}
