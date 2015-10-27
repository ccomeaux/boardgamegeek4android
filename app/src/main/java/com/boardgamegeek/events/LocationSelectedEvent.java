package com.boardgamegeek.events;

public class LocationSelectedEvent {
	private final String locationName;

	public LocationSelectedEvent(String name) {
		this.locationName = name;
	}

	public String getLocationName() {
		return locationName;
	}
}
