package com.boardgamegeek.util;

/**
 * Provides utility methods for dealing with strings.
 */
public class StringUtils {
	// private final static String TAG = "StringUtils";

	public static String createSortName(String name, int sortIndex) {
		if (sortIndex <= 1 || sortIndex > name.length()) {
			return name;
		}
		int i = sortIndex - 1;
		return name.substring(i) + ", " + name.substring(0, i).trim();
	}

	public static int parseInt(String text) {
		return parseInt(text, 0);
	}

	public static int parseInt(String text, int defaultValue) {
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public static double parseDouble(String text) {
		return parseDouble(text, 0);
	}

	public static double parseDouble(String text, double defaultValue) {
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	/**
	 * Gets the ordinal (1st) for the given cardinal (1)
	 * 
	 * @param cardinal
	 * @return
	 */
	public static String getOrdinal(int cardinal) {

		if (cardinal < 0) {
			return "";
		}

		String c = String.valueOf(cardinal);
		String n = "0";
		if (c.length() > 1) {
			n = c.substring(c.length() - 2, c.length() - 1);
		}
		String l = c.substring(c.length() - 1);
		if (!n.equals("1")) {
			if (l.equals("1")) {
				return c + "st";
			} else if (l.equals("2")) {
				return c + "nd";
			} else if (l.equals("3")) {
				return c + "rd";
			}
		}
		return c + "th";
	}

	public static String[] concat(String[] first, String[] second) {
		String[] result = new String[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
}
