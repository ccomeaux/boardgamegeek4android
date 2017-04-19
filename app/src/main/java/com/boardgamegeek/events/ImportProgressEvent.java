package com.boardgamegeek.events;

public class ImportProgressEvent {
	private final String type;

	public ImportProgressEvent(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
}
