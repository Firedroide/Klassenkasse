package ch.kanti_wohlen.klassenkasse.ui.util;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public class CurrencyFormat extends Format {

	public static final CurrencyFormat DEFAULT = new CurrencyFormat("(-?\\d{1,5})(\\.(\\d{1,2}))?");
	public static final CurrencyFormat NON_NEGATIVE = new CurrencyFormat("(\\d{1,5})(\\.(\\d{1,2}))?");

	private final Pattern pattern;

	private CurrencyFormat(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	@Override
	public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
		if (obj == null) return toAppendTo;
		if (!(obj instanceof MonetaryValue)) return toAppendTo;

		MonetaryValue value = (MonetaryValue) obj;
		pos.setBeginIndex(toAppendTo.length());
		toAppendTo.append(value.getAmountString(true));
		pos.setEndIndex(toAppendTo.length());

		return toAppendTo;
	}

	@Override
	public Object parseObject(String source, ParsePosition pos) {
		Matcher m = pattern.matcher(source);
		if (m.find(pos.getIndex())) {
			try {
				long francs = Long.parseLong(m.group(1));
				long cents = 0;
				if (m.group(2) != null) {
					String centString = m.group(3);
					cents = Long.parseLong(m.group(3));
					if (centString.length() == 1) {
						cents *= 10;
					}
				}

				pos.setIndex(m.end());
				return new MonetaryValue(francs, cents);
			} catch (NumberFormatException | ArithmeticException e) {
				pos.setErrorIndex(pos.getIndex());
				return null;
			}
		} else {
			pos.setErrorIndex(pos.getIndex());
			return null;
		}
	}
}