package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeAscendingSortData extends PlayTimeSortData {
	public PlayTimeAscendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.PLAYING_TIME, false);
		mDescriptionId = R.string.menu_collection_sort_playtime_shortest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_PLAY_TIME_ASC;
	}
}
