package com.boardgamegeek.events;

public class PlayChangedEvent {
	private final String gameName;

	public PlayChangedEvent(String gameName) {
		this.gameName = gameName;
	}

	public String getGameName() {
		return gameName;
	}
}
