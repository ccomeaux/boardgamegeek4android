package com.boardgamegeek.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import hugo.weaving.DebugLog;

/**
 * Provides utility methods for dealing with strings.
 */
public class StringUtils {
	private static final int INVALID_RANGE = -1;
	private static final String RANGE_COMMA = ", ";
	private static final String RANGE_DASH = " - ";
	public static final String TRUNCATED_TEXT_SUFFIX = "..";

	private StringUtils() {
	}

	public static String createSortName(String name, int sortIndex) {
		if (sortIndex <= 1 || sortIndex > name.length()) {
			return name;
		}
		int i = sortIndex - 1;
		return name.substring(i) + ", " + name.substring(0, i).trim();
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
		try {
			//noinspection ResultOfMethodCallIgnored
			Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the ordinal (1st) for the given cardinal (1)
	 *
	 * @param cardinal The cardinal number (1, 2, 3)
	 * @return The ordinal number (1st, 2nd, 3rd)
	 */
	public static String getOrdinal(int cardinal) {
		if (cardinal < 0) {
			return "-th";
		}

		String c = String.valueOf(cardinal);
		String n = "0";
		if (c.length() > 1) {
			n = c.substring(c.length() - 2, c.length() - 1);
		}
		String l = c.substring(c.length() - 1);
		if (!n.equals("1")) {
			switch (l) {
				case "1":
					return c + "st";
				case "2":
					return c + "nd";
				case "3":
					return c + "rd";
			}
		}
		return c + "th";
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

	@DebugLog
	public static String formatRange(List<Integer> list) {
		if (list == null) return "";
		if (list.size() == 0) return "";
		if (list.size() == 1) return String.valueOf(list.get(0));

		int first = INVALID_RANGE;
		int last = INVALID_RANGE;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			int current = list.get(i);
			if (first == INVALID_RANGE) {
				first = current;
			} else if (current - 1 == list.get(i - 1)) {
				last = current;
			} else if (last != INVALID_RANGE) {
				if (sb.length() > 0) sb.append(RANGE_COMMA);
				sb.append(first).append(RANGE_DASH).append(last);
				first = INVALID_RANGE;
				last = INVALID_RANGE;
			} else {
				if (sb.length() > 0) sb.append(RANGE_COMMA);
				sb.append(first);
				first = current;
				last = INVALID_RANGE;
			}
		}
		if (first != INVALID_RANGE) {
			if (last != INVALID_RANGE) {
				if (sb.length() > 0) sb.append(RANGE_COMMA);
				sb.append(first).append(RANGE_DASH).append(last);
			} else {
				if (sb.length() > 0) sb.append(RANGE_COMMA);
				sb.append(first);
			}
		}
		return sb.toString();
	}

	/**
	 * Append bold text to a {@link android.text.SpannableStringBuilder}
	 */
	public static void appendBold(SpannableStringBuilder sb, String boldText) {
		sb.append(boldText);
		sb.setSpan(new StyleSpan(Typeface.BOLD), sb.length() - boldText.length(), sb.length(),
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	public static String limitText(String text, int length) {
		if (TextUtils.isEmpty(text)) return "";
		if (text.length() <= length) return text;
		if (length > TRUNCATED_TEXT_SUFFIX.length())
			return text.substring(0, length - TRUNCATED_TEXT_SUFFIX.length()) + TRUNCATED_TEXT_SUFFIX;
		return text.substring(0, length);
	}
}
