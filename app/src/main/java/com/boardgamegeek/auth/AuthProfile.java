package com.boardgamegeek.auth;

import java.util.Date;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

public class AuthProfile {
	public AuthProfile(CookieStore cookieStore) {
		for (Cookie cookie : cookieStore.getCookies()) {
			String name = cookie.getName();
			if (name.equals("bggpassword")) {
				authToken = cookie.getValue();
				authTokenExpiry = getExpiryTime(cookie);
			} else if (name.equals("SessionID")) {
				sessionId = cookie.getValue();
				sessionIdExpiry = getExpiryTime(cookie);
			}
		}
	}

	private long getExpiryTime(Cookie cookie) {
		Date date = cookie.getExpiryDate();
		if (date == null) {
			return 0;
		}
		return date.getTime();
	}

	public String authToken;
	public long authTokenExpiry;
	public String sessionId;
	public long sessionIdExpiry;
}
