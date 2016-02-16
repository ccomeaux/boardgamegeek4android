package com.boardgamegeek.ui.dialog;

import android.content.Context;

import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.interfaces.CollectionView;

public interface CollectionFilterDialog {
	void createDialog(Context context, CollectionView view, CollectionFilterer filter);

	int getType(Context context);
}
