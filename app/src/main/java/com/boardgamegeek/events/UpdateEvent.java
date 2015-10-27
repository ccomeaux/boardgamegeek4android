package com.boardgamegeek.events;

public class UpdateEvent {
	private final int type;

	public UpdateEvent(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}
