package com.boardgamegeek.util;

import android.content.Context;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Constants;

import java.text.DecimalFormat;

import hugo.weaving.DebugLog;

/**
 * Methods to aid in presenting information in a consistent manner.
 */
public class PresentationUtils {
	private static final DecimalFormat RATING_FORMAT = new DecimalFormat("#0.0");
	private static final DecimalFormat AVERAGE_RATING_FORMAT = new DecimalFormat("#0.000");
	private static final DecimalFormat PERSONAL_RATING_FORMAT = new DecimalFormat("#0.#");

	private PresentationUtils() {
	}

	@DebugLog
	public static CharSequence describePastTimeSpan(long time) {
		return describePastTimeSpan(time, "");
	}

	@DebugLog
	public static CharSequence describePastDaySpan(long time) {
		if (time == 0) {
			return "";
		}
		return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS);
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
	public static String describeRating(Context context, double rating) {
		return describeRating(context, rating, RATING_FORMAT);
	}

	@DebugLog
	public static String describeAverageRating(Context context, double rating) {
		return describeRating(context, rating, AVERAGE_RATING_FORMAT);
	}

	@DebugLog
	public static String describePersonalRating(Context context, double rating) {
		return describeRating(context, rating, PERSONAL_RATING_FORMAT);
	}

	@DebugLog
	private static String describeRating(Context context, double rating, DecimalFormat format) {
		if (rating > 0.0) {
			return format.format(rating);
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

	@DebugLog
	public static String describeRankName(Context context, String type, String name) {
		@StringRes int resId = R.string.title_game;
		if (BggService.RANK_TYPE_SUBTYPE.equals(type)) {
			switch (name) {
				case BggService.THING_SUBTYPE_BOARDGAME:
					resId = R.string.title_board_game;
					break;
				case BggService.THING_SUBTYPE_BOARDGAME_EXPANSION:
					resId = R.string.title_expansion;
					break;
				case BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY:
					resId = R.string.title_accessory;
					break;
			}
		} else if (BggService.RANK_TYPE_FAMILY.equals(type)) {
			switch (name) {
				case BggService.RANK_FAMILY_NAME_ABSTRACT_GAMES:
					resId = R.string.title_abstract;
					break;
				case BggService.RANK_FAMILY_NAME_CHILDRENS_GAMES:
					resId = R.string.title_childrens;
					break;
				case BggService.RANK_FAMILY_NAME_CUSTOMIZABLE_GAMES:
					resId = R.string.title_customizable;
					break;
				case BggService.RANK_FAMILY_NAME_FAMILY_GAMES:
					resId = R.string.title_family;
					break;
				case BggService.RANK_FAMILY_NAME_PARTY_GAMES:
					resId = R.string.title_party;
					break;
				case BggService.RANK_FAMILY_NAME_STRATEGY_GAMES:
					resId = R.string.title_strategy;
					break;
				case BggService.RANK_FAMILY_NAME_THEMATIC_GAMES:
					resId = R.string.title_thematic;
					break;
				case BggService.RANK_FAMILY_NAME_WAR_GAMES:
					resId = R.string.title_war;
					break;
				default:
					return name;
			}
		}
		return context.getString(resId);
	}

	@DebugLog
	public static CharSequence describeWeight(Context context, double weight) {
		@StringRes int resId = R.string.weight_1_text;
		if (weight >= 4.2) {
			resId = R.string.weight_5_text;
		} else if (weight >= 3.4) {
			resId = R.string.weight_4_text;
		} else if (weight >= 2.6) {
			resId = R.string.weight_3_text;
		} else if (weight >= 1.8) {
			resId = R.string.weight_2_text;
		}
		return getText(context, resId, weight);
	}

	@DebugLog
	public static String describePlayCount(Context context, int playCount) {
		@StringRes int resId = 0;
		if (playCount >= 100) {
			resId = R.string.play_stat_dollar;
		} else if (playCount >= 50) {
			resId = R.string.play_stat_half_dollar;
		} else if (playCount >= 25) {
			resId = R.string.play_stat_quarter;
		} else if (playCount >= 10) {
			resId = R.string.play_stat_dime;
		} else if (playCount >= 5) {
			resId = R.string.play_stat_nickel;
		}
		if (resId != 0) {
			return context.getString(resId);
		} else {
			return "";
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

	@DebugLog
	public static CharSequence getText(Context context, int id, Object... args) {
		for (int i = 0; i < args.length; ++i) {
			args[i] = args[i] instanceof String ? TextUtils.htmlEncode((String) args[i]) : args[i];
		}
		return Html.fromHtml(String.format(Html.toHtml(new SpannedString(context.getText(id))), args));
	}

	@DebugLog
	public static CharSequence getQuantityText(Context context, int id, int quantity, Object... args) {
		for (int i = 0; i < args.length; ++i) {
			args[i] = args[i] instanceof String ? TextUtils.htmlEncode((String) args[i]) : args[i];
		}
		return Html.fromHtml(String.format(Html.toHtml(new SpannedString(context.getResources().getQuantityText(id, quantity))), args));
	}
}
