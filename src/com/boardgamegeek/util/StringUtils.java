package com.boardgamegeek.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Log;

/**
 * Provides utility methods for dealing with strings.
 */
public class StringUtils {
	private final static String TAG = "StringUtils";
	
	private static Pattern mVertialWhiteSpacePattern;
	
	static {
		mVertialWhiteSpacePattern = Pattern.compile("[\n]{3,}");
	}
	
	public static String formatDescription(String input) {
		if (TextUtils.isEmpty(input)) {
			return null;
		}

		final long startLocal = System.currentTimeMillis();

		String output = input;
		output = convertBreaks(output);
		output = unescapeHtml(output);
		output = trimLineEndings(output);
		output = removeVerticalWhiteSpace(output);

		Log.d(TAG, "Formatting description took " + (System.currentTimeMillis() - startLocal) + "ms");

		return output;
	}

	private static String convertBreaks(String input){
		return input.replace("<br/>", "\n");
	}

	private static String trimLineEndings(String input) {
		return input.replaceAll("[ \t]+$", "");
	}
	
	private static String removeVerticalWhiteSpace(String input){
		Matcher matcher = mVertialWhiteSpacePattern.matcher(input);
		return matcher.replaceAll("\n\n").trim();
	}

	/**
	 * Replaces escaped HTML with the unescaped equivalent.
	 */
	private static String unescapeHtml(String escapedHtml) {
		String unescapedText = escapedHtml;
		unescapedText = unescapedText.replace("&nbsp;", " ");
		unescapedText = unescapedText.replace("&lt;", "<");
		unescapedText = unescapedText.replace("&gt;", ">");
		unescapedText = unescapedText.replace("&amp;", "&");
		unescapedText = unescapedText.replace("&quot;", "\"");
		unescapedText = unescapedText.replace("&ldquo;", "\"");
		unescapedText = unescapedText.replace("&rdquo;", "\"");
		unescapedText = unescapedText.replace("&apos;", "'");
		unescapedText = unescapedText.replace("&lsquo;", "'");
		unescapedText = unescapedText.replace("&rsquo;", "'");
		unescapedText = unescapedText.replace("&acute;", "'");
		unescapedText = unescapedText.replace("&trade;", "™");
		unescapedText = unescapedText.replace("&ndash;", "–");
		unescapedText = unescapedText.replace("&agrave;", "à");
		unescapedText = unescapedText.replace("&Agrave;", "À");
		unescapedText = unescapedText.replace("&acirc;", "â");
		unescapedText = unescapedText.replace("&auml;", "ä");
		unescapedText = unescapedText.replace("&Auml;", "Ä");
		unescapedText = unescapedText.replace("&Acirc;", "Â");
		unescapedText = unescapedText.replace("&aring;", "å");
		unescapedText = unescapedText.replace("&Aring;", "Å");
		unescapedText = unescapedText.replace("&aelig;", "æ");
		unescapedText = unescapedText.replace("&AElig;", "Æ");
		unescapedText = unescapedText.replace("&ccedil;", "ç");
		unescapedText = unescapedText.replace("&Ccedil;", "Ç");
		unescapedText = unescapedText.replace("&eacute;", "é");
		unescapedText = unescapedText.replace("&Eacute;", "É");
		unescapedText = unescapedText.replace("&egrave;", "è");
		unescapedText = unescapedText.replace("&Egrave;", "È");
		unescapedText = unescapedText.replace("&ecirc;", "ê");
		unescapedText = unescapedText.replace("&Ecirc;", "Ê");
		unescapedText = unescapedText.replace("&euml;", "ë");
		unescapedText = unescapedText.replace("&Euml;", "Ë");
		unescapedText = unescapedText.replace("&iuml;", "ï");
		unescapedText = unescapedText.replace("&Iuml;", "Ï");
		unescapedText = unescapedText.replace("&ocirc;", "ô");
		unescapedText = unescapedText.replace("&Ocirc;", "Ô");
		unescapedText = unescapedText.replace("&ouml;", "ö");
		unescapedText = unescapedText.replace("&Ouml;", "Ö");
		unescapedText = unescapedText.replace("&oslash;", "ø");
		unescapedText = unescapedText.replace("&Oslash;", "Ø");
		unescapedText = unescapedText.replace("&szlig;", "ß");
		unescapedText = unescapedText.replace("&ugrave;", "ù");
		unescapedText = unescapedText.replace("&Ugrave;", "Ù");
		unescapedText = unescapedText.replace("&ucirc;", "û");
		unescapedText = unescapedText.replace("&Ucirc;", "Û");
		unescapedText = unescapedText.replace("&uuml;", "ü");
		unescapedText = unescapedText.replace("&Uuml;", "Ü");
		unescapedText = unescapedText.replace("&copy;", "\u00a9");
		unescapedText = unescapedText.replace("&reg;", "\u00ae");
		unescapedText = unescapedText.replace("&euro;", "\u20a0");
		unescapedText = unescapedText.replace("&bull;", "\u0095");
		unescapedText = unescapedText.replace("&#10;", "\n");
		return unescapedText;
	}

	public static String createSortName(String name, int sortIndex) {
		if (sortIndex <= 1 || sortIndex > name.length()) {
			return name;
		}
		int i = sortIndex - 1;
		return name.substring(i) + ", " + name.substring(0, i).trim();
	}
}
