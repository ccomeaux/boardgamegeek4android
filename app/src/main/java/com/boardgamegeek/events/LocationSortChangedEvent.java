package com.boardgamegeek.events;

public class LocationSortChangedEvent {
	public final int sortType;

	public LocationSortChangedEvent(int type) {
		this.sortType = type;
	}
}
