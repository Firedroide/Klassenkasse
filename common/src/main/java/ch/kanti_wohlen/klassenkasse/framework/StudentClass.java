package ch.kanti_wohlen.klassenkasse.framework;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

// TODO: Rename?
@NonNullByDefault
public class StudentClass implements LocallyIdentifiable<Integer> {

	private final int id;

	private String className;
	private MonetaryValue rounding;
	private MonetaryValue balance;

	public StudentClass(Host host, String name) {
		id = host.getIdProvider().generateClassId();
		className = name;
		rounding = MonetaryValue.ZERO;
		balance = MonetaryValue.ZERO;
	}

	public StudentClass(int id, String name, MonetaryValue rounding, MonetaryValue balance) {
		this.id = id;
		this.className = name;
		this.rounding = rounding;
		this.balance = balance;
	}

	@SuppressWarnings("null")
	@Override
	public Integer getLocalId() {
		return id;
	}

	public String getName() {
		return className;
	}

	public void setName(String newName) {
		className = newName;
	}

	public MonetaryValue getRoundingValue() {
		return rounding;
	}

	public void setRoundingValue(MonetaryValue newRoundingValue) {
		rounding = newRoundingValue;
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
