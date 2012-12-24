package com.boardgamegeek.util;

public class UserUrlBuilder extends UrlBuilder {
	private final String mUsername;
	private boolean mBuddies;

	public UserUrlBuilder(String username) {
		mUsername = username;
	}

	public UserUrlBuilder buddies() {
		mBuddies = true;
		return this;
	}

	public String build() {
		String url = BASE_URL_2 + "user?name=" + encode(mUsername);
		if (mBuddies) {
			url += "&buddies=1";
		}
		return url;
	}
}
