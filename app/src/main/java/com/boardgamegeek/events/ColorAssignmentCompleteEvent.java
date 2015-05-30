package com.boardgamegeek.events;

public class ColorAssignmentCompleteEvent {
    public boolean success;

    public ColorAssignmentCompleteEvent(boolean success) {
        this.success = success;
    }
}
