package com.boardgamegeek.auth;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

public class AuthProfile {
	public AuthProfile(CookieStore cookieStore) {
		for (Cookie cookie : cookieStore.getCookies()) {
			String name = cookie.getName();
			if (name.equals("bggpassword")) {
				authToken = cookie.getValue();
				authTokenExpiry = cookie.getExpiryDate().getTime();
			} else if (name.equals("SessionID")) {
				sessionId = cookie.getValue();
				sessionIdExpiry = cookie.getExpiryDate().getTime();
			}
		}
	}

	public String authToken;
	public long authTokenExpiry;
	public String sessionId;
	public long sessionIdExpiry;
}
