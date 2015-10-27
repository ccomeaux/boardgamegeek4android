package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeDescendingSorter extends PlayTimeSorter {
	public PlayTimeDescendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.PLAYING_TIME, true);
		subDescriptionId = R.string.longest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_PLAY_TIME_DESC;
	}
}
