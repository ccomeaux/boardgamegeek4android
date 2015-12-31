package com.boardgamegeek.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Constants;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

/**
 * Methods to aid in presenting information in a consistent manner.
 */
public class PresentationUtils {
	private static final DecimalFormat AVERAGE_RATING_FORMAT = new DecimalFormat("#0.00");
	private static final DecimalFormat RATING_FORMAT = new DecimalFormat("#0.#");
	private static final DecimalFormat MONEY_FORMAT = setUpMoneyFormatter();

	private PresentationUtils() {
	}

	@NonNull
	private static DecimalFormat setUpMoneyFormatter() {
		DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance();
		DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
		symbols.setCurrencySymbol("");
		format.setDecimalFormatSymbols(symbols);
		return format;
	}

	public static CharSequence describePastTimeSpan(long time) {
		return describePastTimeSpan(time, "");
	}

	public static CharSequence describePastTimeSpan(long time, String defaultValue) {
		if (time == 0) {
			return defaultValue;
		}
		return DateUtils.getRelativeTimeSpanString(time);
	}

	public static CharSequence describePastTimeSpan(long time, String defaultValue, String prefix) {
		if (time == 0) {
			return defaultValue;
		}
		return prefix + " " + DateUtils.getRelativeTimeSpanString(time);
	}

	/**
	 * Given the year, return a string interpretation.
	 */
	public static String describeYear(@Nullable Context context, int year) {
		if (context == null) {
			return "";
		}
		if (year > 0) {
			return context.getString(R.string.year_positive, year);
		} else if (year == Constants.YEAR_UNKNOWN) {
			return context.getString(R.string.year_zero, year);
		} else {
			return context.getString(R.string.year_negative, -year);
		}
	}

	/**
	 * Describe the priority of the wishlist.
	 */
	public static String describeWishlist(@Nullable Context context, int priority) {
		if (context == null) {
			return "";
		}
		if (priority < 0 || priority > 5) {
			return context.getString(R.string.wishlist);
		}
		return context.getResources().getStringArray(R.array.wishlist_priority)[priority];
	}

	public static String describeAverageRating(@NonNull Context context, double rating) {
		if (rating > 0.0) {
			return AVERAGE_RATING_FORMAT.format(rating);
		} else {
			return context.getString(R.string.unrated);
		}
	}

	public static String describeRating(@NonNull Context context, double rating) {
		if (rating > 0.0) {
			return RATING_FORMAT.format(rating);
		} else {
			return context.getString(R.string.unrated);
		}
	}

	@NonNull
	public static String describeMoney(String currency, double amount) {
		if (TextUtils.isEmpty(currency) && amount == 0.0) {
			return "";
		}
		return describeCurrency(currency) + MONEY_FORMAT.format(amount);
	}

	@NonNull
	public static String describeMoneyWithoutDecimals(String currency, double amount) {
		if (TextUtils.isEmpty(currency) && amount == 0.0) {
			return "";
		}
		return describeCurrency(currency) + (int) amount;
	}

	private static String describeCurrency(@Nullable String currency) {
		if (currency == null) {
			return "$";
		}
		switch (currency) {
			case "USD":
			case "CAD":
			case "AUD":
				return "$";
			case "EUR":
				return "\u20AC";
			case "GBP":
				return "\u00A3";
			case "YEN":
				return "\u00A5";
		}
		return "";
	}

	/**
	 * Build a displayable full name from the first and last name.
	 */
	@NonNull
	public static String buildFullName(@NonNull String firstName, @NonNull String lastName) {
		if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
			return "";
		} else if (TextUtils.isEmpty(firstName)) {
			return lastName.trim();
		} else if (TextUtils.isEmpty(lastName)) {
			return firstName.trim();
		} else {
			return firstName.trim() + " " + lastName.trim();
		}
	}

	public static void setTextOrHide(@Nullable TextView textView, CharSequence text) {
		if (textView != null) {
			textView.setText(text);
			textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		}
	}
}
