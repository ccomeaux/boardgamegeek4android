package com.boardgamegeek.events;

public class SyncEvent {
	private final int type;

	public SyncEvent(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
