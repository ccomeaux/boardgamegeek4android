package com.boardgamegeek.events;

public class GameSelectedEvent {
	public int id;
	public String name;

	public GameSelectedEvent(int id, String name) {
		this.id = id;
		this.name = name;
	}
}
