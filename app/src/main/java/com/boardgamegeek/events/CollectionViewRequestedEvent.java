package com.boardgamegeek.events;

public class CollectionViewRequestedEvent {
	private final long viewId;

	public CollectionViewRequestedEvent(long viewId) {
		this.viewId = viewId;
	}

	public long getViewId() {
		return viewId;
	}
}
