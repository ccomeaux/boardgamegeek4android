package com.boardgamegeek.events;

public class BuddySelectedEvent {
	public final int buddyId;
	public final String buddyName;
	public final String buddyFullName;

	public BuddySelectedEvent(int id, String name, String fullName) {
		this.buddyId = id;
		this.buddyName = name;
		this.buddyFullName = fullName;
	}
}
