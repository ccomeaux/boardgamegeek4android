package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeAscendingSorter extends PlayTimeSorter {
	public PlayTimeAscendingSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.PLAYING_TIME, false);
		mSubDescriptionId = R.string.shortest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_PLAY_TIME_ASC;
	}
}
