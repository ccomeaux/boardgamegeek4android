package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeAscendingSorter extends PlayTimeSorter {
	public PlayTimeAscendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.PLAYING_TIME, false);
		subDescriptionId = R.string.shortest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_PLAY_TIME_ASC;
	}
}
