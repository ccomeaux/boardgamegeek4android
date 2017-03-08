package com.boardgamegeek.interfaces;

import com.boardgamegeek.filterer.CollectionFilterer;

public interface CollectionView {

	void removeFilter(int type);

	void addFilter(CollectionFilterer filter);
}