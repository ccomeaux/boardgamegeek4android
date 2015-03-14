package com.boardgamegeek.events;

public class LocationSelectedEvent {
	public final String locationName;

	public LocationSelectedEvent(String name) {
		this.locationName = name;
	}
}
