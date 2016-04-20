package com.boardgamegeek.events;

public class PlayerSelectedEvent {
	private final String name;
	private final String username;

	public PlayerSelectedEvent(String name, String username) {
		this.name = name;
		this.username = username;
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}
}
