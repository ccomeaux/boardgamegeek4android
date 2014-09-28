package com.boardgamegeek.sorter;

import com.boardgamegeek.provider.BggContract.Collection;

import android.content.Context;

public abstract class CollectionSorter extends Sorter {
	public CollectionSorter(Context context) {
		super(context);
	}

	@Override
	protected String getDefaultSort() {
		return Collection.DEFAULT_SORT;
	}
}
