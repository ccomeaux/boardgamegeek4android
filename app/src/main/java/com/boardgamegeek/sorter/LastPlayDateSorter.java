package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class LastPlayDateSorter extends CollectionSorter {
	private static final SimpleDateFormat API_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
	@NonNull private final String defaultValue;
	private final String COLUMN_NAME = Plays.MAX_DATE;

	public LastPlayDateSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(COLUMN_NAME, true);
		descriptionId = R.string.collection_sort_play_date_max;
		defaultValue = context.getString(R.string.text_unknown);
	}

	@Override
	public String[] getColumns() {
		return new String[] { COLUMN_NAME };
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_play_date_max;
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		long time = getTime(cursor);
		if (time == DateTimeUtils.UNKNOWN_DATE) return defaultValue;
		return DISPLAY_FORMAT.format(time);
	}

	@NonNull
	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		long time = getTime(cursor);
		if (time == DateTimeUtils.UNKNOWN_DATE) return defaultValue;
		return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString();
	}

	private long getTime(@NonNull Cursor cursor) {
		String date = getString(cursor, COLUMN_NAME, defaultValue);
		return DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, date, API_FORMAT);
	}
}
