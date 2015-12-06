package com.boardgamegeek.interfaces;

import com.boardgamegeek.filterer.CollectionFilterer;

public interface CollectionView {

	void removeFilter(CollectionFilterer filter);

	void addFilter(CollectionFilterer filter);

	void createView(long id, String name);

	void deleteView(long id);
}