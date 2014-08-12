package ch.kanti_wohlen.klassenkasse.util;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Represents a certain amount of money.<br>
 * This class is immutable and checks for long overflows.
 * 
 * @author Roger Baumgartner
 */
public class MonetaryValue implements Cloneable, Comparable<MonetaryValue> {

	public static final @NonNull MonetaryValue ZERO = new MonetaryValue(0);

	private final long value;

	/**
	 * Creates a {@code MonetaryValue} by adding up francs and cents.<br>
	 * Francs can be positive or negative, cents should always be positive.<br>
	 * <br>
	 * <b>Examples:</b>
	 * <ul>
	 * <li>{@code new Money(20, 10)} becomes {@code 20.10}</li>
	 * <li>{@code new Money(20, 120)} becomes {@code 21.20}</li>
	 * <li>{@code new Money(20, -10)} becomes {@code 19.90}</li>
	 * <li>{@code new Money(-15, 10)} becomes {@code -15.10}</li>
	 * <li>{@code new Money(-15, -10)} becomes {@code -14.90}</li>
	 * </ul>
	 * 
	 * @param francs
	 *            The francs for the {@code MonetaryValue}. Can be positive or
	 *            negative.
	 * @param cents
	 *            The cents for this {@code MonetaryValue}. Should always be
	 *            positive.<br>
	 *            Values greater than 99 or smaller than 0 will raise no
	 *            exception.
	 * @throws ArithmeticException
	 *             if a long overflow occurs
	 */
	public MonetaryValue(long francs, long cents) {
		// Avoid overflow due to multiplication with 100
		if (francs > 0 ? francs > Long.MAX_VALUE / 100 : francs < Long.MIN_VALUE / 100) {
			throw new ArithmeticException("Long overflow (multiplication of " + francs + " with 100)");
		}

		// There might also be an overflow due to negating the cent value
		if (francs < 0 && cents == Long.MIN_VALUE) {
			throw new ArithmeticException("Long overflow (negation of " + cents + ")");
		}

		long frCents = francs * 100;
		long adjCents = francs < 0 ? -cents : cents; // -20 and 10 should result
														// in -20.10, not -19.90

		// Check for addition overflow
		if (frCents > 0 ? adjCents > Long.MAX_VALUE - frCents : adjCents < Long.MIN_VALUE - frCents) {
			throw new ArithmeticException("Long overflow (addition of " + frCents + " and " + adjCents + ")");
		}

		// No overflows, calculate value
		value = frCents + adjCents;
	}

	/**
	 * Creates a {@code MonetaryValue} by its cent value.
	 * 
	 * @param cents
	 *            The cents of the {@code MonetaryValue} to be created. Can be
	 *            positive or negative.
	 */
	public MonetaryValue(long cents) {
		value = cents;
	}

	/**
	 * Returns only the whole francs of this {@code MonetaryValue}.<br>
	 * <br>
	 * <b>Examples:</b>
	 * <ul>
	 * <li>{@code 10.90} becomes {@code 10}</li>
	 * <li>{@code 10.00} becomes {@code 10}</li>
	 * <li>{@code -0.05} becomes {@code 0}</li>
	 * <li>{@code -20.90} becomes {@code -20}</li>
	 * </ul>
	 * 
	 * @return the franc value of this {@code MonetaryValue}
	 */
	public long getFrancs() {
		return value / 100;
	}

	/**
	 * Returns only the cents part of this {@code MonetaryValue}.<br>
	 * Will become negative if the whole expression is negative.<br>
	 * <br>
	 * <b>Examples:</b>
	 * <ul>
	 * <li>{@code 10.90} becomes {@code 90}</li>
	 * <li>{@code 10.00} becomes {@code 0}</li>
	 * <li>{@code -0.05} becomes {@code -5}</li>
	 * <li>{@code -20.90} becomes {@code -90}</li>
	 * </ul>
	 * 
	 * @return the cents part of this {@code MonetaryValue}
	 */
	public long getCents() {
		return value % 100;
	}

	/**
	 * Returns the value got by converting this {@code MonetaryValue} to cents
	 * only.<br>
	 * <br>
	 * <b>Examples:</b>
	 * <ul>
	 * <li>{@code 10.90} becomes {@code 1090}</li>
	 * <li>{@code 10.00} becomes {@code 1000}</li>
	 * <li>{@code -0.05} becomes {@code -5}</li>
	 * <li>{@code -20.90} becomes {@code -2090}</li>
	 * </ul>
	 * 
	 * @return this {@code MonetaryValue} converted to cents
	 */
	public long getCentValue() {
		return value;
	}

	/**
	 * Calculates the signum function of this {@code MonetaryValue}.
	 * (The return value is -1 if this is a negative value; 0 if it is zero; and
	 * 1 if is a positive value.)
	 * 
	 * @return the signum function of this {@code MonetaryValue}
	 */
	public int signum() {
		return Long.signum(value);
	}

	/**
	 * @return the negative amount of this {@code MonetaryValue}
	 * @throws ArithmeticException
	 *             if the value is {@code Long.MIN_VALUE}
	 */
	public @NonNull MonetaryValue negate() {
		// Check for negation overflow
		if (value == Long.MIN_VALUE) {
			throw new ArithmeticException("Long overflow (negation of " + value + ")");
		} else if (value == 0) {
			return this;
		}

		return new MonetaryValue(-value);
	}

	/**
	 * Calculates the absolute value of this {@code MonetaryValue}.
	 * If this {@code MonetaryValue} is positive or zero, the same object is
	 * returned.
	 * If it is negative, the negated value is returned.
	 * 
	 * @return the absolute value of this {@code MonetaryValue}.
	 */
	public MonetaryValue abs() {
		if (value < 0) {
			return negate();
		} else {
			return this;
		}
	}

	/**
	 * Adds two {@code MonetaryValue}s together.
	 * 
	 * @param other
	 *            The other {@code MonetaryValue} to add to this value, cannot
	 *            be null
	 * @return A {@code MonetaryValue} representing the sum of the two values
	 * @throws ArithmeticException
	 *             if a long overflow occurs
	 * @throws NullPointerException
	 *             if the other {@code MonetaryValue} is null
	 */
	public @NonNull MonetaryValue add(MonetaryValue other) {
		// Check for addition overflow
		if (value > 0 ? other.value > Long.MAX_VALUE - value : other.value < Long.MIN_VALUE - value) {
			throw new ArithmeticException("Long overflow (addition of " + value + " and " + other.value + ")");
		}

		return new MonetaryValue(value + other.value);
	}

	/**
	 * Subtracts an other {@code MonetaryValue} from this value.
	 * 
	 * @param other
	 *            The other {@code MonetaryValue} to subtract from this value,
	 *            cannot be null
	 * @return A {@code MonetaryValue} representing the difference between the
	 *         two values
	 * @throws ArithmeticException
	 *             if a long overflow occurs
	 * @throws NullPointerException
	 *             if the other {@code MonetaryValue} is null
	 */
	public @NonNull MonetaryValue subtract(MonetaryValue other) {
		// Check for subtraction overflow
		if (value > 0 ? other.value < Long.MIN_VALUE + value : other.value > Long.MAX_VALUE + value) {
			throw new ArithmeticException("Long overflow (subtraction of " + other.value + " from " + value + ")");
		}

		return new MonetaryValue(value - other.value);
	}

	/**
	 * Sums the values of multiple {@code MonetaryValue}s together
	 * 
	 * @param values
	 *            The {@code MonetaryValue}s to be added together. May be null
	 *            or an empty array
	 * @return the sum of all {@code MonetaryValue}s or a value of 0 if
	 *         {@code values} is null or empty
	 * @throws ArithmeticException
	 *             if a long overflow occurs
	 */
	public static @NonNull MonetaryValue sumAll(MonetaryValue... values) {
		if (values == null || values.length == 0) {
			return new MonetaryValue(0);
		}

		MonetaryValue result = new MonetaryValue(0);
		for (MonetaryValue value : values) {
			result = result.add(value);
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof MonetaryValue)) return false;

		return value == ((MonetaryValue) obj).value;
	}

	@Override
	public int hashCode() {
		return (int) value;
	}

	@Override
	public MonetaryValue clone() {
		return new MonetaryValue(value);
	}

	@Override
	public int compareTo(MonetaryValue o) {
		if (o == null) {
			throw new IllegalArgumentException("Cannot compare this MonetaryValue to null");
		}
		return Long.compare(value, o.value);
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder("Fr. ");
		out.append(getFrancs()).append(".");
		out.append(String.format("%02d", getCents()));
		return out.toString();
	}
}
