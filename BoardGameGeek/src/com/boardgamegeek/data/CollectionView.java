package com.boardgamegeek.data;

public interface CollectionView {

	public abstract void removeFilter(CollectionFilterData filter);

	public abstract void addFilter(CollectionFilterData filter);

	public abstract void setSort(int sortType);

	public abstract void setViewName(String name);
}