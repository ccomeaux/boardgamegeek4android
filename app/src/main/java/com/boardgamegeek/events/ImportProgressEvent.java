package com.boardgamegeek.events;

public class ImportProgressEvent {
	private final int requestCode;

	public ImportProgressEvent(int requestCode) {
		this.requestCode = requestCode;
	}

	public int getRequestCode() {
		return requestCode;
	}
}
