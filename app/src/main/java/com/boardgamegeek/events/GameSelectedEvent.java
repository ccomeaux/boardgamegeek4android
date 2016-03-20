package com.boardgamegeek.events;

public class GameSelectedEvent {
	private final int id;
	private final String name;

	public GameSelectedEvent(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
