package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.filterer.CollectionFilterer;

public interface CollectionFilterDialog {
	interface OnFilterChangedListener {

		void removeFilter(int type);

		void addFilter(CollectionFilterer filter);
	}

	void createDialog(Context context, OnFilterChangedListener listener, CollectionFilterer filter);

	int getType(Context context);
}
