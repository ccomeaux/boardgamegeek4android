package com.boardgamegeek.data;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeAscendingSortData extends PlayTimeSortData {
	public PlayTimeAscendingSortData() {
		mOrderByClause = getClause(Collection.PLAYING_TIME, false);
		mDescription = R.string.menu_collection_sort_playtime_shortest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_PLAY_TIME_ASC;
	}
}
