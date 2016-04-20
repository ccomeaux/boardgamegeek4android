package com.boardgamegeek.events;

public class PlayersCountChangedEvent extends CountChangedEvent {
	public PlayersCountChangedEvent(int count) {
		super(count);
	}
}
