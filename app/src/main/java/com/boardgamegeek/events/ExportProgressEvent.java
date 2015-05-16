package com.boardgamegeek.events;

public class ExportProgressEvent {
	public final int totalCount;
	public final int currentCount;

	public ExportProgressEvent(int totalCount, int currentCount) {
		this.totalCount = totalCount;
		this.currentCount = currentCount;
	}
}
