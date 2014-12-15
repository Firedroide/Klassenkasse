package ch.kanti_wohlen.klassenkasse.ui.util;

import java.awt.Color;

import ch.kanti_wohlen.klassenkasse.util.Configuration;
import ch.kanti_wohlen.klassenkasse.util.MonetaryValue;

public final class ConditionalFormatting {

	private ConditionalFormatting() {}

	private static Color high;
	private static Color medium;
	private static Color low;

	private static MonetaryValue classHigh;
	private static MonetaryValue classLow;

	private static MonetaryValue userHigh;
	private static MonetaryValue userLow;

	public static void setConditionalFormatting(Configuration preferences) {
		Configuration colors = preferences.getSubsection("monetaryValues.colors");
		high = new Color(colors.getInteger("high"));
		medium = new Color(colors.getInteger("medium"));
		low = new Color(colors.getInteger("low"));

		Configuration classes = preferences.getSubsection("monetaryValues.class");
		classHigh = new MonetaryValue(classes.getLong("high"));
		classLow = new MonetaryValue(classes.getLong("low"));

		Configuration users = preferences.getSubsection("monetaryValues.user");
		userHigh = new MonetaryValue(users.getLong("high"));
		userLow = new MonetaryValue(users.getLong("low"));
	}

	public static Color getClassColor(MonetaryValue balance) {
		if (balance.compareTo(classHigh) >= 0) {
			return high;
		} else if (balance.compareTo(classLow) == -1) {
			return low;
		} else {
			return medium;
		}
	}

	public static Color getUserColor(MonetaryValue balance) {
		if (balance.compareTo(userHigh) >= 0) {
			return high;
		} else if (balance.compareTo(userLow) == -1) {
			return low;
		} else {
			return medium;
		}
	}
}
