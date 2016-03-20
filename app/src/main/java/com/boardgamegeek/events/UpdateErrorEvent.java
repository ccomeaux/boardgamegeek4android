package com.boardgamegeek.events;

public class UpdateErrorEvent {
	private final String message;

	public UpdateErrorEvent(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
