package com.boardgamegeek.events;

public class PlaysSortChangedEvent {
	public String description;

	public PlaysSortChangedEvent(String description) {
		this.description = description;
	}
}
