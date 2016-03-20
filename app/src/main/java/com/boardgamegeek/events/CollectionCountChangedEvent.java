package com.boardgamegeek.events;

public class CollectionCountChangedEvent {
	private final int count;

	public CollectionCountChangedEvent(int count) {
		this.count = count;
	}

	public int getCount() {
		return count;
	}
}
