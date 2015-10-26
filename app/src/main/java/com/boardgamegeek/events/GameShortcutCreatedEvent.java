package com.boardgamegeek.events;

public class GameShortcutCreatedEvent {
	public int id;
	public String name;
	public String thumbnailUrl;

	public GameShortcutCreatedEvent(int id, String name, String thumbnailUrl) {
		this.id = id;
		this.name = name;
		this.thumbnailUrl = thumbnailUrl;
	}
}
