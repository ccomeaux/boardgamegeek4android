package com.boardgamegeek.data.sort;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class YearPublishedAscendingSortData extends YearPublishedSortData {
	public YearPublishedAscendingSortData(Context context) {
		super(context);
		mOrderByClause = getClause(Collection.YEAR_PUBLISHED, false);
		mSubDescriptionId = R.string.oldest;
	}

	@Override
	public int getType() {
		return CollectionSortDataFactory.TYPE_YEAR_PUBLISHED_ASC;
	}
}
