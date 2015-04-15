package com.boardgamegeek.events;

public abstract class CountChangedEvent {
	public final int count;

	public CountChangedEvent(int count) {
		this.count = count;
	}
}
