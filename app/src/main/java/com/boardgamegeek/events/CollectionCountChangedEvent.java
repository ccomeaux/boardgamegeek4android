package com.boardgamegeek.events;

public class CollectionCountChangedEvent {
	public int count;

	public CollectionCountChangedEvent(int count) {
		this.count = count;
	}
}
