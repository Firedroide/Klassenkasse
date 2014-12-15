package ch.kanti_wohlen.klassenkasse.framework;

import org.eclipse.jdt.annotation.Nullable;

import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public final class StudentClass implements LocallyIdentifiable<Integer> {

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

	public MonetaryValue getRoundedBalance() {
		return balance;
	}

	public void setRawBalance(MonetaryValue newBalance) {
		balance = newBalance;
	}

	public MonetaryValue getBalance() {
		return balance.add(rounding);
	}

//	public MonetaryValue recalculateBalance(Host host) {
//		Map<Integer, User> users = host.getUsersByClass(id);
//		MonetaryValue result = new MonetaryValue(0);
//
//		for (User user : users.values()) {
//			result = result.add(user.getBalance());
//		}
//		return result;
//	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj instanceof StudentClass) {
			return id == ((StudentClass) obj).id;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return id;
	}
}
