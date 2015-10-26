package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountAscendingSorter extends PlayCountSorter {
	public PlayCountAscendingSorter(Context context) {
		super(context);
		orderByClause = getClause(Collection.NUM_PLAYS, false);
		descriptionId = R.string.menu_collection_sort_played_least;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_PLAY_COUNT_ASC;
	}
}
