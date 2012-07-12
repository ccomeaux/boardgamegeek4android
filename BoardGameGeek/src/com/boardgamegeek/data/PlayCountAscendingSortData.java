package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountAscendingSortData extends PlayCountSortData {
	public PlayCountAscendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.NUM_PLAYS, false);
		mDescriptionId = R.string.menu_collection_sort_played_least;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_PLAY_COUNT_ASC;
	}
}
