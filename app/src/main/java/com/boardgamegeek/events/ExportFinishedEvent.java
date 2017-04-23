package com.boardgamegeek.events;

public class ExportFinishedEvent {
	private final String errorMessage;
	private final String type;

	public ExportFinishedEvent(String type, String errorMessage) {
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
