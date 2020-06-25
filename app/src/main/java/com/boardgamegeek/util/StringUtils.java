package com.boardgamegeek.util;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides utility methods for dealing with strings.
 */
public class StringUtils {
	public static final String TRUNCATED_TEXT_SUFFIX = "..";

	private StringUtils() {
	}

	/**
	 * Parse a string to an int, returning 0 if it's not parsable.
	 */
	public static int parseInt(String text) {
		return parseInt(text, 0);
	}

	/**
	 * Parse a string to an int, returning the default value if it's not parsable.
	 */
	public static int parseInt(String text, int defaultValue) {
		if (TextUtils.isEmpty(text)) return defaultValue;
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException | NullPointerException ex) {
			return defaultValue;
		}
	}

	/**
	 * Parse a string to an long, returning 0 if it's not parsable.
	 */
	public static long parseLong(String text) {
		return parseLong(text, 0);
	}

	/**
	 * Parse a string to an long, returning the default value if it's not parsable.
	 */
	public static long parseLong(String text, int defaultValue) {
		if (TextUtils.isEmpty(text)) return defaultValue;
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException | NullPointerException ex) {
			return defaultValue;
		}
	}

	/**
	 * Parse a string to an double, returning the 0.0 if it's not parsable.
	 */
	public static double parseDouble(String text) {
		return parseDouble(text, 0.0);
	}

	/**
	 * Parse a string to an double, returning the default value if it's not parsable.
	 */
	public static double parseDouble(String text, double defaultValue) {
		if (TextUtils.isEmpty(text)) return defaultValue;
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException | NullPointerException ex) {
			return defaultValue;
		}
	}

	/**
	 * Determines if the string can be converted to a number.
	 */
	public static boolean isNumeric(String text) {
		if (TextUtils.isEmpty(text)) return false;
		try {
			Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/**
	 * Concatenates 2 arrays of strings into 1.
	 */
	public static String[] concatenate(String[] array1, String[] array2) {
		if (array1 == null) return array2;
		if (array2 == null) return array1;
		String[] result = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, result, 0, array1.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);
		return result;
	}

	/**
	 * Returns a union of 2 arrays, ensuring that each string exists only once.
	 */
	public static String[] unionArrays(String[] array1, String[] array2) {
		if (array1 == null) return array2;
		if (array2 == null) return array1;
		Set<String> set = new LinkedHashSet<>();
		set.addAll(Arrays.asList(array1));
		set.addAll(Arrays.asList(array2));
		return set.toArray(new String[set.size()]);
	}

	/**
	 * Formats a list of items with commas and ampersands where necessary.
	 */
	public static <E> String formatList(List<E> list) {
		return formatList(list, "&", ",");
	}

	public static <E> String formatList(List<E> list, String and, final String comma) {
		StringBuilder sb = new StringBuilder();
		if (list != null && list.size() > 0) {
			if (list.size() == 1) {
				sb.append(list.get(0));
			} else if (list.size() == 2) {
				sb.append(list.get(0)).append(" ").append(and).append(" ").append(list.get(1));
			} else {
				for (int i = 0; i < list.size(); i++) {
					sb.append(list.get(i));
					if (i == list.size() - 2) {
						sb.append(comma).append(" ").append(and).append(" ");
					} else if (i < list.size() - 2) {
						sb.append(comma).append(" ");
					}
				}
			}
		}
		return sb.toString();
	}

	public static String limitText(String text, int length) {
		if (TextUtils.isEmpty(text)) return "";
		if (text.length() <= length) return text;
		if (length > TRUNCATED_TEXT_SUFFIX.length())
			return text.substring(0, length - TRUNCATED_TEXT_SUFFIX.length()) + TRUNCATED_TEXT_SUFFIX;
		return text.substring(0, length);
	}

	public static String repeat(String string, int count) {
		if (TextUtils.isEmpty(string)) return "";
		if (count < 0) return string;

		final int len = string.length();
		final int size = len * count;

		final char[] array = new char[size];
		string.getChars(0, len, array, 0);
		int n;
		for (n = len; n < size - n; n <<= 1) {
			System.arraycopy(array, 0, array, n, n);
		}
		System.arraycopy(array, 0, array, n, size - n);
		return new String(array);
	}
}
