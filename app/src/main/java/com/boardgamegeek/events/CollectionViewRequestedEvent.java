package com.boardgamegeek.events;

public class CollectionViewRequestedEvent {
	public long viewId;

	public CollectionViewRequestedEvent(long viewId) {
		this.viewId = viewId;
	}
}
