package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeDescendingSortData extends PlayTimeSortData {
	public PlayTimeDescendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.PLAYING_TIME, true);
		mDescriptionId = R.string.menu_collection_sort_playtime_longest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_PLAY_TIME_DESC;
	}
}
