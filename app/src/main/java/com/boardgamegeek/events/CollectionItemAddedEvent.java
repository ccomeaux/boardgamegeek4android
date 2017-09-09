package com.boardgamegeek.events;

public class CollectionItemAddedEvent {
	private final int gameId;

	public CollectionItemAddedEvent(int gameId) {
		this.gameId = gameId;
	}

	public long getGameId() {
		return gameId;
	}
}
