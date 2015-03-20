package com.boardgamegeek.events;

public class UpdateErrorEvent {
	public String message;

	public UpdateErrorEvent(String message) {
		this.message = message;
	}
}
