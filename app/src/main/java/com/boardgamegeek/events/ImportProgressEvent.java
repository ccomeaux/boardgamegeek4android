package com.boardgamegeek.events;

public class ImportProgressEvent {
	private final int totalCount;
	private final int currentCount;

	public ImportProgressEvent(int totalCount, int currentCount) {
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
