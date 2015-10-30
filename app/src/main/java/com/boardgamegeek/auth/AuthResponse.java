package com.boardgamegeek.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

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

	@NonNull
	public static AuthResponse getMock() {
		AuthResponse authResponse = new AuthResponse();
		authResponse.authToken = "password";
		authResponse.authTokenExpiry = Long.MAX_VALUE;
		authResponse.sessionId = "session";
		authResponse.sessionIdExpiry = Long.MAX_VALUE;
		return authResponse;
	}

	@Nullable
	public static AuthResponse fromCookieStore(@Nullable CookieStore cookieStore) {
		if (cookieStore == null || cookieStore.getCookies() == null || cookieStore.getCookies().isEmpty()) {
			Timber.w("missing cookies");
			return null;
		}

		AuthResponse authResponse = createAuthResponse(cookieStore.getCookies());
		if (authResponse.isValid()) {
			return authResponse;
		} else {
			return null;
		}
	}

	@NonNull
	private static AuthResponse createAuthResponse(List<HttpCookie> cookies) {
		AuthResponse authResponse = new AuthResponse();
		for (HttpCookie cookie : cookies) {
			String name = cookie.getName();
			if (name.equals("bggpassword")) {
				authResponse.authToken = cookie.getValue();
				authResponse.authTokenExpiry = getExpiryTime(cookie);
			} else if (name.equals("SessionID")) {
				authResponse.sessionId = cookie.getValue();
				authResponse.sessionIdExpiry = getExpiryTime(cookie);
			}
		}
		return authResponse;
	}

	private boolean isValid() {
		return !TextUtils.isEmpty(authToken);
	}

	private static long getExpiryTime(@NonNull HttpCookie cookie) {
		return System.currentTimeMillis() + cookie.getMaxAge() * 1000;
	}

	@NonNull
	@Override
	public String toString() {
		return String.format("token: %s (%s); session: %s (%s)", authToken, new Date(authTokenExpiry), sessionIdExpiry, new Date(sessionIdExpiry));
	}
}
