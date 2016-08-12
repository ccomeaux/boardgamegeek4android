package com.boardgamegeek.events;

public class CollectionItemUpdatedEvent {
	private final long internalId;

	public CollectionItemUpdatedEvent() {
		this.internalId = 0;
	}

	public CollectionItemUpdatedEvent(long internalId) {
		this.internalId = internalId;
	}

	public long getInternalId() {
		return internalId;
	}
}
