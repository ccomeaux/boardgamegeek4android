package com.boardgamegeek.events;

public class ExportProgressEvent {
	private final int totalCount;
	private final int currentCount;
	private final int stepIndex;

	public ExportProgressEvent(int totalCount, int currentCount, int stepIndex) {
		this.totalCount = totalCount;
		this.currentCount = currentCount;
		this.stepIndex = stepIndex;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int getCurrentCount() {
		return currentCount;
	}

	public int getStepIndex() {
		return stepIndex;
	}
}
