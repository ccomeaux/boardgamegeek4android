package com.boardgamegeek.events;

public class CollectionItemDeletedEvent {
	private final long internalId;

	public CollectionItemDeletedEvent(long internalId) {
		this.internalId = internalId;
	}

	public long getInternalId() {
		return internalId;
	}
}
