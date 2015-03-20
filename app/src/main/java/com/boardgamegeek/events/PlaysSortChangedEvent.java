package com.boardgamegeek.events;

public class PlaysSortChangedEvent {
	public int type;
	public String description;

	public PlaysSortChangedEvent(int type, String description) {
		this.type = type;
		this.description = description;
	}
}
