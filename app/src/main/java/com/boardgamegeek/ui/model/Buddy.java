package com.boardgamegeek.ui.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.PresentationUtils;

public class Buddy {
	public static final String[] PROJECTION = {
		Buddies._ID,
		Buddies.BUDDY_ID,
		Buddies.BUDDY_NAME,
		Buddies.BUDDY_FIRSTNAME,
		Buddies.BUDDY_LASTNAME,
		Buddies.AVATAR_URL,
		Buddies.PLAY_NICKNAME,
		Buddies.UPDATED
	};

	private static final int BUDDY_ID = 1;
	private static final int BUDDY_NAME = 2;
	private static final int FIRST_NAME = 3;
	private static final int LAST_NAME = 4;
	private static final int AVATAR_URL = 5;
	private static final int NICKNAME = 6;
	private static final int UPDATED = 7;

	private int buddyId;
	private String firstName;
	private String lastName;
	private String buddyName;
	private String avatarUrl;
	private String nickName;
	private long updated;
	private String fullName;

	public static Buddy fromCursor(Cursor cursor) {
		Buddy buddy = new Buddy();
		buddy.buddyId = cursor.getInt(BUDDY_ID);
		buddy.firstName = cursor.getString(FIRST_NAME);
		buddy.lastName = cursor.getString(LAST_NAME);
		buddy.buddyName = cursor.getString(BUDDY_NAME);
		buddy.avatarUrl = cursor.getString(AVATAR_URL);
		buddy.nickName = cursor.getString(NICKNAME);
		buddy.updated = cursor.getLong(UPDATED);
		buddy.fullName = PresentationUtils.buildFullName(buddy.firstName, buddy.lastName);
		return buddy;
	}

	public int getId() {
		return buddyId;
	}

	public String getUserName() {
		return buddyName;
	}

	public String getNickName() {
		return nickName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getFullName() {
		return fullName;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public long getUpdated() {
		return updated;
	}
}
