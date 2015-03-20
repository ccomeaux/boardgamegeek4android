package com.boardgamegeek.events;

public class CollectionStatusChangedEvent {
	public String description;

	public CollectionStatusChangedEvent(String description) {
		this.description = description;
	}
}
