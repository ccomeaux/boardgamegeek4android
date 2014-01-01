package com.boardgamegeek.data;

public interface CollectionView {

	public abstract void removeFilter(CollectionFilterData filter);

	public abstract void addFilter(CollectionFilterData filter);

	public abstract void createView(long id, String name);
	
	public abstract void deleteView(long id);
}