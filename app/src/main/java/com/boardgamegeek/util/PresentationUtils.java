package com.boardgamegeek.util;

import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.R;

/**
 * Methods to aid in presenting information in a consistent manner.
 */
public class PresentationUtils {
	private PresentationUtils() {
	}

	/**
	 * Given the year, return a string interpretation.
	 */
	public static String describeYear(Context context, int year) {
		if (year > 0) {
			return context.getString(R.string.year_positive, year);
		} else if (year == 0) {
			return context.getString(R.string.year_zero, year);
		} else {
			return context.getString(R.string.year_negative, -year);
		}
	}

	/**
	 * Describe the priority of the wishlist.
	 */
	public static String describeWishlist(Context context, int priority) {
		if (priority < 0 || priority > 5) {
			return context.getString(R.string.wishlist);
		}
		return context.getResources().getStringArray(R.array.wishlist_priority)[priority];
	}
}
