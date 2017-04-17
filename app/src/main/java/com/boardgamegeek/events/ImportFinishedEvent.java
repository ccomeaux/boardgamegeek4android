package com.boardgamegeek.events;

public class ImportFinishedEvent {
	private final String errorMessage;

	public ImportFinishedEvent(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
