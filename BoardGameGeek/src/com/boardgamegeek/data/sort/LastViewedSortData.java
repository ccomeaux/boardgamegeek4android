package com.boardgamegeek.data.sort;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;

public class LastViewedSortData extends CollectionSortData {
	private String mNever;

	public LastViewedSortData(Context context) {
		super(context);
		mDescriptionId = R.string.menu_collection_sort_last_viewed;
		mOrderByClause = getClause(Games.LAST_VIEWED, true);
		mNever = context.getString(R.string.never);
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_LAST_VIEWED;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Games.LAST_VIEWED };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		long time = getLong(cursor, Games.LAST_VIEWED);
		if (time == 0) {
			return mNever;
		}
		return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS)
			.toString();
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		long time = getLong(cursor, Games.LAST_VIEWED);
		if (time == 0) {
			return mNever;
		}
		return DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
			.toString();
	}
}
