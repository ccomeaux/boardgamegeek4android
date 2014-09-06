package com.boardgamegeek.sorter;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeDescendingSorter extends PlayTimeSorter {
	public PlayTimeDescendingSorter(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.PLAYING_TIME, true);
		mSubDescriptionId = R.string.longest;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_PLAY_TIME_DESC;
	}
}
