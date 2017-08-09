package com.boardgamegeek.events;

public class GameShortcutRequestedEvent {
	private final int id;
	private final String name;
	private final String thumbnailUrl;

	public GameShortcutRequestedEvent(int id, String name, String thumbnailUrl) {
		this.id = id;
		this.name = name;
		this.thumbnailUrl = thumbnailUrl;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}
}
