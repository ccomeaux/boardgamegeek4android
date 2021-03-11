package com.boardgamegeek.util;

import android.text.TextUtils;

import java.text.NumberFormat;
import java.text.ParseException;

/**
 * Provides utility methods for dealing with strings.
 */
public class StringUtils {
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
	 * Parse a string to an double, returning the default value if it's not parsable.
	 */
	public static double parseDouble(String text, double defaultValue) {
		if (TextUtils.isEmpty(text)) return defaultValue;
		try {
			Number parsed = NumberFormat.getNumberInstance().parse(text);
			if (parsed == null) return defaultValue;
			return parsed.doubleValue();
		} catch (ParseException | NullPointerException ex) {
			return defaultValue;
		}
	}
}
