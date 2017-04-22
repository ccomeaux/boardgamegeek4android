package com.boardgamegeek.events;

public class ExportProgressEvent {
	private final int totalCount;
	private final int currentCount;
	private final String type;

	public ExportProgressEvent(int totalCount, int currentCount, String type) {
		this.totalCount = totalCount;
		this.currentCount = currentCount;
		this.type = type;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int getCurrentCount() {
		return currentCount;
	}

	public String getType() {
		return type;
	}
}
