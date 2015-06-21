package com.boardgamegeek.events;

public class ImportProgressEvent {
	public final int totalCount;
	public final int currentCount;

	public ImportProgressEvent(int totalCount, int currentCount) {
		this.totalCount = totalCount;
		this.currentCount = currentCount;
	}
}
