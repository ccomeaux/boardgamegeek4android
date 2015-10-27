package com.boardgamegeek.events;

public class LocationSortChangedEvent {
	private final int sortType;

	public LocationSortChangedEvent(int type) {
		this.sortType = type;
	}

	public int getSortType() {
		return sortType;
	}
}
