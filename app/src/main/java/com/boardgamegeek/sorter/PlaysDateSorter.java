package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

import timber.log.Timber;

public class PlaysDateSorter extends PlaysSorter {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

	public PlaysDateSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.menu_plays_sort_date;
	}

	@Override
	public int getType() {
		return PlaysSorterFactory.TYPE_PLAY_DATE;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Plays.DATE };
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		String date = getString(cursor, Plays.DATE);
		long millis = DateTimeUtils.getMillisFromApiDate(date, 0);
		if (millis == 0) {
			Timber.w("This isn't a date in the expected format: " + date);
			return date;
		}
		return DATE_FORMAT.format(millis);
	}
}
