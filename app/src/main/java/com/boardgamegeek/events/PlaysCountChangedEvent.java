package com.boardgamegeek.events;

public class PlaysCountChangedEvent extends CountChangedEvent {
	public PlaysCountChangedEvent(int count) {
		super(count);
	}
}
