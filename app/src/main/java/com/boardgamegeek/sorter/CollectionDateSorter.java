package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.util.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

public abstract class CollectionDateSorter extends CollectionSorter {
	private static final SimpleDateFormat API_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
	private final String defaultValue;

	public CollectionDateSorter(@NonNull Context context) {
		super(context);
		defaultValue = context.getString(R.string.text_unknown);
	}

	@Override
	protected boolean isSortDescending() {
		return true;
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		long time = getTime(cursor);
		if (time == DateTimeUtils.UNKNOWN_DATE) return defaultValue;
		return DISPLAY_FORMAT.format(time);
	}

	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		long time = getTime(cursor);
		if (time == DateTimeUtils.UNKNOWN_DATE) return defaultValue;
		return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString();
	}

	private long getTime(@NonNull Cursor cursor) {
		String date = getString(cursor, getSortColumn(), defaultValue);
		return DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, date, API_FORMAT);
	}
}
