package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

// TODO: Rename?
@NonNullByDefault
public class StudentClass {

	private final int id;

	private String className;
	private MonetaryValue balance;

	public StudentClass(Host host, String name) {
		id = host.getIdProvider().generateClassId();
		className = name;
		balance = MonetaryValue.ZERO;
	}

	public StudentClass(int id, String name, MonetaryValue balance) {
		this.id = id;
		this.className = name;
		this.balance = balance;
	}

	public int getLocalId() {
		return id;
	}

	public String getName() {
		return className;
	}

	public void setName(String newName) {
		className = newName;
	}

	public MonetaryValue getBalance() {
		return balance;
	}

	public MonetaryValue recalculateBalance(Host host) {
		Map<Integer, User> users = host.getUsersByClass(id);
		MonetaryValue result = new MonetaryValue(0);

		for (User user : users.values()) {
			result = result.add(user.getBalance());
		}
		return result;
	}
}
