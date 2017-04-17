package com.boardgamegeek.events;

public class ExportProgressEvent {
	private final int totalCount;
	private final int currentCount;

	public ExportProgressEvent(int totalCount, int currentCount) {
		this.totalCount = totalCount;
		this.currentCount = currentCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int getCurrentCount() {
		return currentCount;
	}
}
