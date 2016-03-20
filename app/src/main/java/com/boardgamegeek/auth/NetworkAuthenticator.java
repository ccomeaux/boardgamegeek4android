package com.boardgamegeek.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.boardgamegeek.util.HttpUtils;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class NetworkAuthenticator {
	@SuppressWarnings("FieldCanBeLocal") private static final boolean MOCK_LOGIN = false;

	private NetworkAuthenticator() {
	}

	/**
	 * Authenticates to BGG with the specified username and password, returning the cookie store to use on subsequent
	 * requests, or null if authentication fails.
	 */
	@Nullable
	public static AuthResponse authenticate(@NonNull String username, @NonNull String password) {
		if (MOCK_LOGIN) {
			return AuthResponse.getMock();
		} else {
			return tryAuthenticate(username, password);
		}
	}

	@Nullable
	private static AuthResponse tryAuthenticate(@NonNull String username, @NonNull String password) {
		try {
			return performAuthenticate(username, password);
		} catch (@NonNull final IOException e) {
			Timber.w(e, "IOException when attempting to authenticate");
		} finally {
			Timber.w("Authentication complete");
		}
		return null;
	}

	@Nullable
	@DebugLog
	private static AuthResponse performAuthenticate(@NonNull String username, @NonNull String password) throws IOException {
		final OkHttpClient client = HttpUtils.getHttpClient();
		CookieManager cookieManager = createCookieManager(client);
		Request post = buildRequest(username, password);
		final Response response = client.newCall(post).execute();
		return determineResponseSuccess(cookieManager, response);
	}

	@DebugLog
	@NonNull
	private static Request buildRequest(@NonNull String username, @NonNull String password) {
		RequestBody formBody = new FormEncodingBuilder()
			.add("username", username)
			.add("password", password)
			.build();
		return new Request.Builder()
			.url("https://www.boardgamegeek.com/login")
			.post(formBody)
			.build();
	}

	@DebugLog
	@NonNull
	private static CookieManager createCookieManager(@NonNull OkHttpClient client) {
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		client.setCookieHandler(cookieManager);
		return cookieManager;
	}

	@DebugLog
	@Nullable
	private static AuthResponse determineResponseSuccess(@NonNull CookieManager cookieManager, @NonNull Response response) {
		if (response.isSuccessful()) {
			Timber.w("Successful authentication");
			return AuthResponse.fromCookieStore(cookieManager.getCookieStore());
		} else {
			Timber.w("Bad response code - " + response.code());
			return null;
		}
	}
}
