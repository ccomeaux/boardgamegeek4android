package com.boardgamegeek.events;

public class ImportFinishedEvent {
	private final String errorMessage;
	private final int requestCode;

	public ImportFinishedEvent(int requestCode, String errorMessage) {
		this.requestCode = requestCode;
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public int getRequestCode() {
		return requestCode;
	}
}
