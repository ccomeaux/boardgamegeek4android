package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountDescendingSorter extends PlayCountSorter {
	public PlayCountDescendingSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.NUM_PLAYS, true);
		mDescriptionId = R.string.menu_collection_sort_played_most;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_PLAY_COUNT_DESC;
	}
}
