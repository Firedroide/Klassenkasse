package ch.kanti_wohlen.klassenkasse.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;

import ch.kanti_wohlen.klassenkasse.action.UpdateType;
import ch.kanti_wohlen.klassenkasse.framework.Host;
import ch.kanti_wohlen.klassenkasse.framework.Payment;
import ch.kanti_wohlen.klassenkasse.framework.StudentClass;
import ch.kanti_wohlen.klassenkasse.framework.User;

@NonNullByDefault
public final class PaymentHelper {

	private PaymentHelper() {}

	public static void updatePaymentValue(Host host, int paymentId, MonetaryValue oldValue, MonetaryValue newValue) {
		Map<Integer, User> users = host.getUsersWithPayment(paymentId);
		if (users.isEmpty()) return;

		MonetaryValue change = newValue.subtract(oldValue);
		for (User user : users.values()) {
			user.setBalance(user.getBalance().add(change));
			host.updateUser(user, UpdateType.UPDATE);
		}

		@SuppressWarnings("null")
		Map<Integer, Integer> classMap = mapUsersToClasses(users.values());
		for (Entry<Integer, Integer> entry : classMap.entrySet()) {
			StudentClass studentClass = host.getClassById(entry.getKey());
			if (studentClass == null) throw new IllegalStateException("Inexistant StudentClass");

			MonetaryValue classChange = change.multiply(entry.getValue());
			studentClass.setRawBalance(studentClass.getRoundedBalance().add(classChange));
			host.updateClass(studentClass, UpdateType.UPDATE);
		}
	}

	public static void changeRoundingValue(Host host, int paymentId, MonetaryValue newRounding) {
		Payment oldPayment = host.getPaymentById(paymentId);
		MonetaryValue oldRounding;
		if (oldPayment == null) {
			oldRounding = MonetaryValue.ZERO;
		} else {
			oldRounding = oldPayment.getRoundingValue();
		}

		Map<Integer, User> paymentUsers = host.getUsersWithPayment(paymentId);
		@SuppressWarnings("null")
		Map<Integer, Integer> classMap = mapUsersToClasses(paymentUsers.values());

		if (classMap.size() == 0) return;
		MonetaryValue roundingChange = newRounding.subtract(oldRounding);

		if (classMap.size() == 1) {
			// Simple case if only 1 class is affected:
			// Subtract the old rounding value from the class and add the new one
			StudentClass studentClass = host.getClassById(classMap.keySet().iterator().next());
			if (studentClass == null) throw new IllegalStateException("Inexistant StudentClass");

			MonetaryValue rounding = studentClass.getRoundingValue().add(roundingChange);
			studentClass.setRoundingValue(rounding);
			host.updateClass(studentClass, UpdateType.UPDATE);
		} else {
			Map<Integer, MonetaryValue> roundingValues = getRoundingValuesPerClass(classMap, roundingChange);
			for (Entry<Integer, MonetaryValue> entry : roundingValues.entrySet()) {
				StudentClass studentClass = host.getClassById(entry.getKey());
				if (studentClass == null) throw new IllegalStateException("Inexistant StudentClass");

				MonetaryValue rounding = studentClass.getRoundingValue().add(entry.getValue());
				studentClass.setRoundingValue(rounding);
				host.updateClass(studentClass, UpdateType.UPDATE);
			}
		}
	}

	public static void changePaymentUsersRounding(Host host, Payment payment, Collection<User> changedUsers, boolean remove) {
		MonetaryValue roundingValue = payment.getRoundingValue();
		if (roundingValue.equals(MonetaryValue.ZERO)) return;

		Collection<User> oldUsers = host.getUsersWithPayment(payment.getLocalId()).values();
		Collection<User> newUsers = new ArrayList<>(oldUsers);
		if (remove) {
			newUsers.removeAll(changedUsers);
		} else {
			newUsers.addAll(changedUsers);
		}

		@SuppressWarnings("null")
		Map<Integer, Integer> oldClassMap = mapUsersToClasses(oldUsers);
		Map<Integer, Integer> newClassMap = mapUsersToClasses(newUsers);

		Map<Integer, MonetaryValue> oldRoundingMap = getRoundingValuesPerClass(oldClassMap, roundingValue);
		Map<Integer, MonetaryValue> newRoundingMap = getRoundingValuesPerClass(newClassMap, roundingValue);

		Map<Integer, StudentClass> studentClasses = new HashMap<>();
		for (Entry<Integer, MonetaryValue> sub : oldRoundingMap.entrySet()) {
			StudentClass studentClass = host.getClassById(sub.getKey());
			if (studentClass == null) throw new IllegalStateException("Inexistant StudentClass");

			MonetaryValue rounding = studentClass.getRoundingValue().subtract(sub.getValue());
			studentClass.setRoundingValue(rounding);
			studentClasses.put(studentClass.getLocalId(), studentClass);
		}
		for (Entry<Integer, MonetaryValue> add : newRoundingMap.entrySet()) {
			StudentClass studentClass = studentClasses.get(add.getKey());
			if (studentClass == null) {
				studentClass = host.getClassById(add.getKey());
				if (studentClass == null) throw new IllegalStateException("Inexistant StudentClass");

				studentClasses.put(studentClass.getLocalId(), studentClass);
			}

			MonetaryValue rounding = studentClass.getRoundingValue().add(add.getValue());
			studentClass.setRoundingValue(rounding);
			studentClasses.put(studentClass.getLocalId(), studentClass);
		}

		for (StudentClass studentClass : studentClasses.values()) {
			if (studentClass == null) continue;
			host.updateClass(studentClass, UpdateType.UPDATE);
		}
	}

	public static void changePaymentUsersBalance(Host host, Payment payment, Collection<User> changedUsers, boolean remove) {
		Map<Integer, StudentClass> studentClasses = new HashMap<>();
		for (User user : changedUsers) {
			MonetaryValue userBalance;
			if (remove) {
				userBalance = user.getBalance().subtract(payment.getValue());
			} else {
				userBalance = user.getBalance().add(payment.getValue());
			}
			user.setBalance(userBalance);
			host.updateUser(user, UpdateType.UPDATE);

			StudentClass studentClass = studentClasses.get(user.getStudentClassId());
			if (studentClass == null) {
				studentClass = user.getStudentClass(host);
				studentClasses.put(studentClass.getLocalId(), studentClass);
			}

			MonetaryValue classBalance;
			if (remove) {
				classBalance = studentClass.getRoundedBalance().subtract(payment.getValue());
			} else {
				classBalance = studentClass.getRoundedBalance().add(payment.getValue());
			}
			studentClass.setRawBalance(classBalance);
		}

		for (StudentClass studentClass : studentClasses.values()) {
			if (studentClass == null) continue;
			host.updateClass(studentClass, UpdateType.UPDATE);
		}
	}

	/**
	 * Returns either the rounded up or the rounded down value of this {@link MonetaryValue},
	 * whichever reduces the amount of the stored class rounding values the most.
	 * 
	 * @param host the {@link Host} to look up the {@linkplain StudentClass StudentClasses}
	 * @param oldRounding the old rounding value of the {@link Payment} of which the rounding value is to be changed
	 * @param value the {@code MonetaryValue} to be rounded
	 * @param oldUsers the old {@linkplain User Users} of this {@code Payment}
	 * @param newUsers the new {@code Users} of this {@code Payment}
	 * @return the rounded {@code MonetaryValue}, either rounded up or down
	 */
	public static MonetaryValue getBestRoundingValue(Host host, MonetaryValue value, MonetaryValue oldRounding,
			Collection<User> oldUsers, Collection<User> newUsers) {
		Set<Integer> studentClassIds = new HashSet<>();
		for (User user : oldUsers) studentClassIds.add(user.getStudentClassId());
		for (User user : newUsers) studentClassIds.add(user.getStudentClassId());

		MonetaryValue allCombined = MonetaryValue.ZERO;
		for (Integer classId : studentClassIds) {
			if (classId < 0) continue; // TODO: Temp fix

			StudentClass studentClass = host.getClassById(classId);
			if (studentClass == null) throw new IllegalStateException("Cannot view StudentClass");

			allCombined = allCombined.add(studentClass.getRoundingValue());
		}
		allCombined = allCombined.subtract(oldRounding);

		MonetaryValue up = roundToClosest(value, 5 * newUsers.size(), true);
		MonetaryValue down = roundToClosest(value, 5 * newUsers.size(), false);

		MonetaryValue allRoundingUp = allCombined.add(value.subtract(up)).abs();
		MonetaryValue allRoundingDown = allCombined.add(value.subtract(down)).abs();

		if (allRoundingUp.compareTo(allRoundingDown) > 0) {
			return down;
		} else {
			return up;
		}
	}

	/**
	 * Rounds a {@link MonetaryValue} to the closest {@code amount} of cents.
	 * 
	 * @param value the {@code MonetaryValue} to be rounded
	 * @param amount how many cents {@code value} should be rounded to, should be multiples of {@code 5}
	 * @param roundUp whether {@code value} should be rounded up, down otherwise.
	 * @return the rounded {@code MonetaryValue}
	 */
	private static MonetaryValue roundToClosest(MonetaryValue value, long amount, boolean roundUp) {
		long cents = value.getCentValue();
		long rounded;
		if (roundUp) {
			rounded = amount * Math.round(Math.ceil((double) cents / amount));
		} else {
			rounded = amount * Math.round(Math.floor((double) cents / amount));
		}

		return new MonetaryValue(rounded);
	}

	/**
	 * Calculates how many users are {@linkplain User Users} of this {@link Payment} are in each {@link StudentClass}.
	 * 
	 * @param users a {@link Collection} of {@code User}s, not {@code null}
	 * @return a {@link Map} mapping {@code StudentClass ID} --> number of {@code User}s in the {@code StudentClass}
	 */
	private static Map<Integer, Integer> mapUsersToClasses(Collection<User> users) {
		Map<Integer, Integer> result = new HashMap<>();
		for (User user : users) {
			int classId = user.getStudentClassId();
			if (result.containsKey(classId)) {
				result.put(classId, result.get(classId) + 1);
			} else {
				result.put(classId, 1);
			}
		}
		return result;
	}

	/**
	 * Calculates the rounding value for each {@link StudentClass}.
	 * <p>
	 * It is insignificant how accurate this method actually is,
	 * as the maximum difference is 1 cent.
	 * </p>
	 * <p>
	 * It is only important that
	 * <ul>
	 * <li>the rounding values add up to {@code roundingValue}</li>
	 * <li>this method always returns the same distribution of the rounding value</li>
	 * </ul>
	 * </p>
	 * 
	 * @param classMap a {@link Map} mapping {@code StudentClass} IDs to the amount of {@linkplain User Users}.
	 * @param roundingValue the combined value that needs to be rounded
	 * @return a {@code Map} mapping {@code StudentClass} IDs to their rounding values.
	 */
	private static Map<Integer, MonetaryValue> getRoundingValuesPerClass(Map<Integer, Integer> classMap, MonetaryValue roundingValue) {
		Map<Integer, MonetaryValue> result = new HashMap<>();
		TreeMap<Integer, Integer> sorted = new TreeMap<>(classMap);

		MonetaryValue remainingRounding = roundingValue;
		int remainingUsers = 0;
		for (Integer value : classMap.values()) {
			remainingUsers += value;
		}

		for (Entry<Integer, Integer> entry : sorted.entrySet()) {
			long rounding = Math.round(remainingRounding.getCentValue() * ((double) entry.getValue() / remainingUsers));
			MonetaryValue value = new MonetaryValue(rounding);
			remainingRounding = remainingRounding.subtract(value);
			remainingUsers -= entry.getValue();

			result.put(entry.getKey(), value);
		}

		return result;
	}
}
