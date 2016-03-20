package com.boardgamegeek.events;

public class CollectionStatusChangedEvent {
	private final String description;

	public CollectionStatusChangedEvent(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
