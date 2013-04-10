package com.boardgamegeek.data;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountDescendingSortData extends PlayCountSortData {
	public PlayCountDescendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.NUM_PLAYS, true);
		mDescriptionId = R.string.menu_collection_sort_played_most;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_PLAY_COUNT_DESC;
	}
}
