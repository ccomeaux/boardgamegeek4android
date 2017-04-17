package com.boardgamegeek.events;

public class ExportProgressEvent {
	private final int totalCount;
	private final int currentCount;
	private final int requestCode;

	public ExportProgressEvent(int totalCount, int currentCount, int requestCode) {
		this.totalCount = totalCount;
		this.currentCount = currentCount;
		this.requestCode = requestCode;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int getCurrentCount() {
		return currentCount;
	}

	public int getRequestCode() {
		return requestCode;
	}
}
