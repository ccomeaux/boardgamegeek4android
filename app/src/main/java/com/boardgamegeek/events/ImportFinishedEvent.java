package com.boardgamegeek.events;

public class ImportFinishedEvent {
	private final String errorMessage;
	private final String type;

	public ImportFinishedEvent(String type, String errorMessage) {
		this.errorMessage = errorMessage;
		this.type = type;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getType() {
		return type;
	}
}
