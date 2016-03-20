package com.boardgamegeek.events;

public abstract class CountChangedEvent {
	private final int count;

	public CountChangedEvent(int count) {
		this.count = count;
	}

	public int getCount() {
		return count;
	}
}
