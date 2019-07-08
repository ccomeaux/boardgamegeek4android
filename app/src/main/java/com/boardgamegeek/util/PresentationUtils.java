package com.boardgamegeek.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Html;
import android.text.SpannedString;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ConstantsKt;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import hugo.weaving.DebugLog;

/**
 * Methods to aid in presenting information in a consistent manner.
 */
public class PresentationUtils {
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

	public static String describeYear(@Nullable Context context, int year) {
		if (context == null) return "";
		if (year > 0) {
			return context.getString(R.string.year_positive, String.valueOf(year));
		} else if (year == ConstantsKt.YEAR_UNKNOWN) {
			return context.getString(R.string.year_zero);
		} else {
			return context.getString(R.string.year_negative, String.valueOf(-year));
		}
	}

	@NonNull
	public static String describeMoney(String currency, double amount) {
		if (TextUtils.isEmpty(currency) && amount == 0.0) return "";
		return describeCurrency(currency) + MONEY_FORMAT.format(amount);
	}

	private static String describeCurrency(@Nullable String currency) {
		if (currency == null) return "$";
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

	public static void setTextOrHide(@Nullable TextView view, int number) {
		if (view != null) {
			view.setText(String.valueOf(number));
			view.setVisibility(number == 0 ? View.GONE : View.VISIBLE);
		}
	}

	public static void setTextOrHide(@Nullable TextView view, CharSequence text) {
		if (view != null) {
			view.setText(text);
			view.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		}
	}

	public static CharSequence getText(Context context, @StringRes int id, Object... args) {
		for (int i = 0; i < args.length; ++i) {
			args[i] = args[i] instanceof String ? TextUtils.htmlEncode((String) args[i]) : args[i];
		}
		final String htmlString = String.format(Html.toHtml(new SpannedString(context.getText(id))), args);
		return trimTrailingWhitespace(Html.fromHtml(htmlString));
	}

	public static CharSequence trimTrailingWhitespace(CharSequence source) {
		if (source == null) return "";

		int i = source.length();
		do {
			--i;
		} while (i >= 0 && Character.isWhitespace(source.charAt(i)));

		return source.subSequence(0, i + 1);
	}

	public static int[] getColorSchemeResources() {
		return new int[] { R.color.orange, R.color.light_blue, R.color.dark_blue, R.color.light_blue };
	}

	public static void colorFab(FloatingActionButton fab, @ColorInt int iconColor) {
		if (fab != null && iconColor != Color.TRANSPARENT) {
			fab.setBackgroundTintList(ColorStateList.valueOf(iconColor));
		}
	}

	@NonNull
	public static String getHttpErrorMessage(Context context, int httpCode) {
		@StringRes int resId;
		if (httpCode >= 500) resId = R.string.msg_sync_response_500;
		else if (httpCode == 429) resId = R.string.msg_sync_response_429;
		else if (httpCode == 202) resId = R.string.msg_sync_response_202;
		else resId = R.string.msg_sync_error_http_code;
		return context.getString(resId, String.valueOf(httpCode));
	}
}
