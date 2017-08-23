package com.boardgamegeek.events;

public class CollectionItemResetEvent {
	private final long internalId;

	public CollectionItemResetEvent(long internalId) {
		this.internalId = internalId;
	}

	public long getInternalId() {
		return internalId;
	}
}
