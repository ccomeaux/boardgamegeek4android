package com.boardgamegeek.auth;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class AuthResponse {
	// TODO private setters
	public String authToken;
	public long authTokenExpiry;
	public String sessionId;
	public long sessionIdExpiry;

	private AuthResponse() {
	}

	public static AuthResponse getMock() {
		AuthResponse authResponse = new AuthResponse();
		authResponse.authToken = "password";
		authResponse.authTokenExpiry = Long.MAX_VALUE;
		authResponse.sessionId = "session";
		authResponse.sessionIdExpiry = Long.MAX_VALUE;
		return authResponse;
	}

	public static AuthResponse fromCookieStore(CookieStore cookieStore) {
		if (cookieStore == null) {
			Timber.w("missing cookies");
			return null;
		}
		List<HttpCookie> cookies = cookieStore.getCookies();
		if (cookies == null || cookies.isEmpty()) {
			Timber.w("missing cookies");
			return null;
		}

		boolean isValid = false;
		AuthResponse authResponse = new AuthResponse();
		for (HttpCookie cookie : cookies) {
			String name = cookie.getName();
			if (name.equals("bggpassword")) {
				authResponse.authToken = cookie.getValue();
				authResponse.authTokenExpiry = getExpiryTime(cookie);
				isValid = true;
			} else if (name.equals("SessionID")) {
				authResponse.sessionId = cookie.getValue();
				authResponse.sessionIdExpiry = getExpiryTime(cookie);
			}
		}
		if (isValid) {
			return authResponse;
		} else {
			return null;
		}
	}

	private static long getExpiryTime(HttpCookie cookie) {
		long seconds = cookie.getMaxAge();
		return System.currentTimeMillis() + seconds * 1000;
	}

	@Override
	public String toString() {
		return "token: " + authToken + " (" + new Date(authTokenExpiry) + ")" +
			"; session: " + sessionIdExpiry + " (" + new Date(sessionIdExpiry) + ")";
	}
}
