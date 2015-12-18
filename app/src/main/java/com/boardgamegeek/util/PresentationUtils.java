package com.boardgamegeek.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Constants;

import java.text.DecimalFormat;

import hugo.weaving.DebugLog;

/**
 * Methods to aid in presenting information in a consistent manner.
 */
public class PresentationUtils {
	private static final DecimalFormat AVERAGE_RATING_FORMAT = new DecimalFormat("#0.000");
	private static final DecimalFormat RATING_FORMAT = new DecimalFormat("#0.#");

	private PresentationUtils() {
	}

	@DebugLog
	public static CharSequence describePastTimeSpan(long time) {
		return describePastTimeSpan(time, "");
	}

	@DebugLog
	public static CharSequence describePastTimeSpan(long time, String defaultValue) {
		if (time == 0) {
			return defaultValue;
		}
		return DateUtils.getRelativeTimeSpanString(time);
	}

	@DebugLog
	public static CharSequence describePastTimeSpan(long time, String defaultValue, String prefix) {
		if (time == 0) {
			return defaultValue;
		}
		return prefix + " " + DateUtils.getRelativeTimeSpanString(time);
	}

	@DebugLog
	public static String describeYear(Context context, int year) {
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

	@DebugLog
	public static String describeWishlist(Context context, int priority) {
		if (context == null) {
			return "";
		}
		if (priority < 0 || priority > 5) {
			return context.getString(R.string.wishlist);
		}
		return context.getResources().getStringArray(R.array.wishlist_priority)[priority];
	}

	@DebugLog
	public static String describeAverageRating(Context context, double rating) {
		if (rating > 0.0) {
			return AVERAGE_RATING_FORMAT.format(rating);
		} else {
			return context.getString(R.string.unrated);
		}
	}

	@DebugLog
	public static String describeRating(Context context, double rating) {
		if (rating > 0.0) {
			return RATING_FORMAT.format(rating);
		} else {
			return context.getString(R.string.unrated);
		}
	}

	@DebugLog
	public static String describeRank(int rank) {
		if (rank == 0 || rank == Integer.MAX_VALUE) {
			return "";
		} else {
			return "#" + rank;
		}
	}

	/**
	 * Build a displayable full name from the first and last name.
	 */
	@DebugLog
	public static String buildFullName(String firstName, String lastName) {
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

	@DebugLog
	public static void setTextOrHide(TextView textView, CharSequence text) {
		if (textView != null) {
			textView.setText(text);
			textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		}
	}
}
