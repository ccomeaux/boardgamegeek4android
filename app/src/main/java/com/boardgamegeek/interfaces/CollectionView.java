package com.boardgamegeek.interfaces;

import com.boardgamegeek.filterer.CollectionFilterer;

public interface CollectionView {

	public abstract void removeFilter(CollectionFilterer filter);

	public abstract void addFilter(CollectionFilterer filter);

	public abstract void createView(long id, String name);
	
	public abstract void deleteView(long id);
}