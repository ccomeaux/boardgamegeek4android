package com.boardgamegeek.events;

public class PlaysSortChangedEvent {
	private final int type;
	private final String description;

	public PlaysSortChangedEvent(int type, String description) {
		this.type = type;
		this.description = description;
	}

	public int getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}
}
