package com.boardgamegeek.events;

public class ExportProgressEvent {
	public final int totalCount;
	public final int currentCount;
	public final int stepIndex;

	public ExportProgressEvent(int totalCount, int currentCount, int stepIndex) {
		this.totalCount = totalCount;
		this.currentCount = currentCount;
		this.stepIndex = stepIndex;
	}
}
