package com.boardgamegeek.events;

public class BuddySelectedEvent {
	private final int buddyId;
	private final String buddyName;
	private final String buddyFullName;

	public BuddySelectedEvent(int id, String name, String fullName) {
		this.buddyId = id;
		this.buddyName = name;
		this.buddyFullName = fullName;
	}

	public int getBuddyId() {
		return buddyId;
	}

	public String getBuddyName() {
		return buddyName;
	}

	public String getBuddyFullName() {
		return buddyFullName;
	}
}
