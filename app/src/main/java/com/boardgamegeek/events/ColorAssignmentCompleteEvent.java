package com.boardgamegeek.events;

public class ColorAssignmentCompleteEvent {
    private final boolean isSuccessful;

    public ColorAssignmentCompleteEvent(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

	public boolean isSuccessful() {
		return isSuccessful;
	}
}
