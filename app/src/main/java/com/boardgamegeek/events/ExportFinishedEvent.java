package com.boardgamegeek.events;

public class ExportFinishedEvent {
	private final String errorMessage;

	public ExportFinishedEvent(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
