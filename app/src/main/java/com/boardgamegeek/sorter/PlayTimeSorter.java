package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public abstract class PlayTimeSorter extends CollectionSorter {
	public PlayTimeSorter(@NonNull Context context) {
		super(context);
	}

	@StringRes
	@Override
	protected int getDescriptionId() {
		return R.string.collection_sort_play_time;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.PLAYING_TIME };
	}

	@NonNull
	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		int minutes = getInt(cursor, Collection.PLAYING_TIME);
		if (minutes == 0) {
			return "?";
		}
		if (minutes >= 120) {
			return (minutes / 60) + " " + context.getString(R.string.hours_abbr);
		} else {
			return getIntAsString(cursor, Collection.PLAYING_TIME, "?") + " " + context.getString(R.string.minutes_abbr);
		}
	}

	@NonNull
	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		return getIntAsString(cursor, Collection.PLAYING_TIME, "?") + " " + context.getString(R.string.minutes);
	}
}
