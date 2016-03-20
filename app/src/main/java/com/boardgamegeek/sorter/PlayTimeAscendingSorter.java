package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class PlayTimeAscendingSorter extends PlayTimeSorter {
	public PlayTimeAscendingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(Collection.PLAYING_TIME, false);
		subDescriptionId = R.string.shortest;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_play_time_asc;
	}
}
