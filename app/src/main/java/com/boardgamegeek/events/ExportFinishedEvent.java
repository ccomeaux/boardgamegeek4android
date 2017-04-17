package com.boardgamegeek.events;

public class ExportFinishedEvent {
	private final String errorMessage;
	private final int requestCode;

	public ExportFinishedEvent(int requestCode, String errorMessage) {
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
