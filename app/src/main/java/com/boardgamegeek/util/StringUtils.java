package com.boardgamegeek.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides utility methods for dealing with strings.
 */
public class StringUtils {

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
		} catch (NumberFormatException | NullPointerException ex) {
			return defaultValue;
		}
	}

	public static double parseDouble(String text) {
		return parseDouble(text, 0.0);
	}

	public static double parseDouble(String text, double defaultValue) {
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException | NullPointerException ex) {
			return defaultValue;
		}
	}

	public static boolean isInteger(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isNumeric(String text) {
		try {
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

	public static String[] concat(String[] first, String[] second) {
		String[] result = new String[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static String[] unionArrays(String[] array1, String[] array2) {
		if (array1 == null) {
			return array2;
		}
		if (array2 == null) {
			return array1;
		}
		Set<String> set = new LinkedHashSet<>();
		set.addAll(Arrays.asList(array1));
		set.addAll(Arrays.asList(array2));
		return set.toArray(new String[set.size()]);
	}

	public static <E> String formatList(List<E> list) {
		StringBuilder sb = new StringBuilder();
		if (list != null && list.size() > 0) {
			if (list.size() == 1) {
				sb.append(list.get(0));
			} else if (list.size() == 2) {
				sb.append(list.get(0)).append(" & ").append(list.get(1));
			} else {
				for (int i = 0; i < list.size(); i++) {
					sb.append(list.get(i));
					if (i == list.size() - 2) {
						sb.append(", & ");
					} else if (i < list.size() - 2) {
						sb.append(", ");
					}
				}
			}
		}
		return sb.toString();
	}

	public static SpannableString boldSecondString(String first, String second) {
		return boldSecondString(first, second, "");
	}

	public static SpannableString boldSecondString(String first, String second, String third) {
		String formattableMessage = first + " " + second + " " + third;
		SpannableString ss = new SpannableString(formattableMessage.trim());
		int length = first.length() + 1;
		ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), length, length + second.length(),
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return ss;
	}

	public static void appendBold(SpannableStringBuilder sb, String boldText) {
		sb.append(boldText);
		sb.setSpan(new StyleSpan(Typeface.BOLD), sb.length() - boldText.length(), sb.length(),
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
}
