package com.boardgamegeek.events;

public class PlaysFilterChangedEvent {
	public int type;
	public String description;

	public PlaysFilterChangedEvent(int type, String description) {
		this.type = type;
		this.description = description;
	}
}
