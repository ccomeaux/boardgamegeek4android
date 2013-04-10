package com.boardgamegeek.data;

import java.util.List;


public interface CollectionView {

	public abstract void removeFilter(CollectionFilterData filter);

	public abstract void addFilter(CollectionFilterData filter);

	public abstract void setSort(int sortType);
	
	public abstract void setFilterName(String name, boolean saveName);
	
	public abstract void setFilters(List<CollectionFilterData> filters);
}