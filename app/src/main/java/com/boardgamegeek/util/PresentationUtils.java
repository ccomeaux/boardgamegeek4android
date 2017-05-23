package com.boardgamegeek.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.text.Html;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Constants;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import hugo.weaving.DebugLog;

/**
 * Methods to aid in presenting information in a consistent manner.
 */
public class PresentationUtils {
	private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("#0.0#");
	private static final DecimalFormat AVERAGE_RATING_FORMAT = new DecimalFormat("#0.000");
	private static final DecimalFormat PERSONAL_RATING_FORMAT = new DecimalFormat("#0.#");
	private static final DecimalFormat MONEY_FORMAT = setUpMoneyFormatter();

	private PresentationUtils() {
	}

	@DebugLog
	@NonNull
	private static DecimalFormat setUpMoneyFormatter() {
		DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance();
		DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
		symbols.setCurrencySymbol("");
		format.setDecimalFormatSymbols(symbols);
		return format;
	}

	/**
	 * Formats the date for display in the forums (based on the users selected preference.
	 */
	public static CharSequence formatTimestamp(Context context, long date, boolean isForumTimestamp, boolean includeTime) {
		int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH;
		if (includeTime) flags |= DateUtils.FORMAT_SHOW_TIME;
		if (isForumTimestamp && PreferencesUtils.getForumDates(context)) {
			return DateUtils.formatDateTime(context, date, flags);
		} else {
			if (date == 0) {
				return context.getString(R.string.text_unknown);
			}
			return DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags);
		}
	}

	@DebugLog
	public static CharSequence describePastDaySpan(long time) {
		if (time == 0) {
			return "";
		}
		return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS);
	}

	@DebugLog
	public static String describeYear(@Nullable Context context, int year) {
		if (context == null) {
			return "";
		}
		if (year > 0) {
			return context.getString(R.string.year_positive, String.valueOf(year));
		} else if (year == Constants.YEAR_UNKNOWN) {
			return context.getString(R.string.year_zero);
		} else {
			return context.getString(R.string.year_negative, String.valueOf(-year));
		}
	}

	@DebugLog
	public static String describeWishlist(@Nullable Context context, int priority) {
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
		return describeScore(context, rating, R.string.unrated);
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
	private static String describeRating(@NonNull Context context, double rating, DecimalFormat format) {
		if (rating > 0.0) {
			return format.format(rating);
		} else {
			return context.getString(R.string.unrated);
		}
	}

	@DebugLog
	public static String describeScore(@NonNull Context context, double score) {
		return describeScore(context, score, 0);
	}

	@DebugLog
	public static String describeScore(@NonNull Context context, double score, @StringRes int defaultResId) {
		if (score > 0.0) {
			return SCORE_FORMAT.format(score);
		} else if (defaultResId > 0) {
			return context.getString(defaultResId);
		}
		return "";
	}

	@DebugLog
	public static CharSequence describePlayerAge(Context context, int age) {
		if (age <= 0) return context.getString(R.string.ages_unknown);
		return getText(context, R.string.age_prefix, age);
	}

	@DebugLog
	public static CharSequence describePlayerRange(Context context, int minPlayers, int maxPlayers) {
		if (minPlayers == 0 && maxPlayers == 0) {
			return context.getResources().getString(R.string.player_range_unknown);
		} else if (minPlayers >= maxPlayers) {
			return getQuantityText(context, R.plurals.player_range_suffix, minPlayers, minPlayers);
		} else {
			return getText(context, R.string.player_range_suffix, minPlayers, maxPlayers);
		}
	}

	@DebugLog
	public static CharSequence describeMinuteRange(Context context, int min, int max, int defaultMins) {
		if (min == 0 && max == 0) return describeMinutes(context, defaultMins);
		if (min == 0) return describeMinutes(context, max);
		if (max == 0) return describeMinutes(context, min);
		return getText(context, R.string.mins_range_suffix, min, max);
	}

	@DebugLog
	private static CharSequence describeMinutes(Context context, int minutes) {
		if (minutes == 0) return context.getString(R.string.mins_unknown);

		if (minutes >= 120) {
			int hours = minutes / 60;
			int remainingMinutes = minutes % 60;

			if (remainingMinutes == 0) {
				return getText(context, R.string.hrs_suffix, hours);
			} else {
				return getText(context, R.string.hrs_mins, hours, remainingMinutes);
			}
		} else {
			return getText(context, R.string.mins_suffix, minutes);
		}
	}

	@DebugLog
	public static CharSequence describeRank(Context context, int rank, String type, String name) {
		if (rank == 0 || rank == Integer.MAX_VALUE) {
			return describeRankName(context, type, name);
		} else {
			return getText(context, R.string.rank_description, rank, describeRankName(context, type, name));
		}
	}

	@DebugLog
	private static CharSequence describeRankName(Context context, String type, String name) {
		if (name == null) return "";
		if (type == null) return name;
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
				default:
					return name;
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
		return context.getText(resId);
	}

	@DebugLog
	public static CharSequence describeWeight(@NonNull Context context, double weight) {
		@StringRes int resId = R.string.unknown;
		if (weight >= 4.5 && weight <= 5.0) {
			resId = R.string.weight_5_text;
		} else if (weight >= 3.5) {
			resId = R.string.weight_4_text;
		} else if (weight > 2.5) {
			resId = R.string.weight_3_text;
		} else if (weight > 1.5) {
			resId = R.string.weight_2_text;
		} else if (weight >= 1.0) {
			resId = R.string.weight_1_text;
		}
		return context.getText(resId);
	}

	@DebugLog
	public static CharSequence describeLanguageDependence(@NonNull Context context, double value) {
		@StringRes int resId = R.string.unknown;
		if (value >= 4.5 && value <= 5.0) {
			resId = R.string.language_5_text;
		} else if (value >= 3.5) {
			resId = R.string.language_4_text;
		} else if (value > 2.5) {
			resId = R.string.language_3_text;
		} else if (value > 1.5) {
			resId = R.string.language_2_text;
		} else if (value >= 1.0) {
			resId = R.string.language_1_text;
		}
		return context.getText(resId);
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
	@DebugLog
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

	@DebugLog
	public static String describePlayer(String name, String username) {
		if (TextUtils.isEmpty(username)) {
			return name;
		}
		return name + " (" + username + ")";
	}

	@DebugLog
	@NonNull
	public static String describePlayDetails(Context context, String date, String location, int quantity, int length, int playerCount) {
		String info = "";
		if (quantity > 1) {
			info += quantity + " " + context.getString(R.string.times) + " ";
		}
		if (!TextUtils.isEmpty(date)) {
			info += context.getString(R.string.on) + " " + date + " ";
		}
		if (!TextUtils.isEmpty(location)) {
			info += context.getString(R.string.at) + " " + location + " ";
		}
		if (length > 0) {
			info += context.getString(R.string.for_) + " " + DateTimeUtils.formatMinutes(length) + " ";
		}
		if (playerCount > 0) {
			info += context.getResources().getQuantityString(R.plurals.player_description, playerCount, playerCount);
		}
		return info.trim();
	}

	@DebugLog
	public static void setTextOrHide(@Nullable TextView view, CharSequence text) {
		if (view != null) {
			view.setText(text);
			view.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		}
	}

	public static void setAndSelectExistingText(@Nullable EditText view, @Nullable String existingText) {
		if (view != null && !TextUtils.isEmpty(existingText)) {
			view.setText(existingText);
			view.setSelection(0, existingText.length());
		}
	}

	@DebugLog
	public static CharSequence getText(Context context, @StringRes int id, Object... args) {
		for (int i = 0; i < args.length; ++i) {
			args[i] = args[i] instanceof String ? TextUtils.htmlEncode((String) args[i]) : args[i];
		}
		final String htmlString = String.format(Html.toHtml(new SpannedString(context.getText(id))), args);
		return trimTrailingWhitespace(Html.fromHtml(htmlString));
	}

	@DebugLog
	public static CharSequence getQuantityText(Context context, @PluralsRes int id, int quantity, Object... args) {
		for (int i = 0; i < args.length; ++i) {
			args[i] = args[i] instanceof String ? TextUtils.htmlEncode((String) args[i]) : args[i];
		}
		final String htmlString = String.format(Html.toHtml(new SpannedString(context.getResources().getQuantityText(id, quantity))), args);
		return trimTrailingWhitespace(Html.fromHtml(htmlString));
	}

	@DebugLog
	public static CharSequence trimTrailingWhitespace(CharSequence source) {
		if (source == null) return "";

		int i = source.length();
		do {
			--i;
		} while (i >= 0 && Character.isWhitespace(source.charAt(i)));

		return source.subSequence(0, i + 1);
	}

	@DebugLog
	public static int[] getColorSchemeResources() {
		return new int[] { R.color.orange, R.color.light_blue, R.color.dark_blue, R.color.light_blue };
	}

	@DebugLog
	public static void ensureFabIsShown(final FloatingActionButton fab) {
		fab.postDelayed(new Runnable() {
			@Override
			public void run() {
				fab.show();
			}
		}, 2000);
	}
}
