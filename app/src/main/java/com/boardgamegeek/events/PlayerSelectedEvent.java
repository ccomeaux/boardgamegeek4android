package com.boardgamegeek.events;

public class PlayerSelectedEvent {
	private final String displayName;
	private final String username;

	public PlayerSelectedEvent(String displayName, String username) {
		this.displayName = displayName;
		this.username = username;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getUsername() {
		return username;
	}
}
