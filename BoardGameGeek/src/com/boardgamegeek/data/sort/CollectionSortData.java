package com.boardgamegeek.data.sort;

import com.boardgamegeek.provider.BggContract.Collection;

import android.content.Context;

public abstract class CollectionSortData extends SortData {
	public CollectionSortData(Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return Collection.DEFAULT_SORT;
	}
}
