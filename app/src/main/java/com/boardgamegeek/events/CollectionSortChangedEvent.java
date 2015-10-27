package com.boardgamegeek.events;

public class CollectionSortChangedEvent {
	private final String name;

	public CollectionSortChangedEvent(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
