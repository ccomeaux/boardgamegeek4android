package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayCountDescendingSorter extends PlayCountSorter {
	public PlayCountDescendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.NUM_PLAYS, true);
		descriptionId = R.string.menu_collection_sort_played_most;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_PLAY_COUNT_DESC;
	}
}
