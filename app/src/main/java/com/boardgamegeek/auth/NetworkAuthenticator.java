package com.boardgamegeek.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.boardgamegeek.util.HttpUtils;

import java.io.IOException;

import hugo.weaving.DebugLog;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
	public static BggCookieJar authenticate(@NonNull String username, @NonNull String password) {
		if (MOCK_LOGIN) {
			return BggCookieJar.getMock();
		} else {
			return tryAuthenticate(username, password);
		}
	}

	@Nullable
	private static BggCookieJar tryAuthenticate(@NonNull String username, @NonNull String password) {
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
	private static BggCookieJar performAuthenticate(@NonNull String username, @NonNull String password) throws IOException {
		final BggCookieJar cookieJar = new BggCookieJar();
		final OkHttpClient client = HttpUtils.getHttpClient().newBuilder()
			.cookieJar(cookieJar)
			.build();
		Request post = buildRequest(username, password);
		final Response response = client.newCall(post).execute();
		if (response.isSuccessful() && cookieJar.isValid()) {
			Timber.w("Successful authentication");
			return cookieJar;
		} else {
			Timber.w("Bad response code - " + response.code());
			return null;
		}
	}

	@DebugLog
	@NonNull
	private static Request buildRequest(@NonNull String username, @NonNull String password) {
		FormBody formBody = new FormBody.Builder()
			.add("username", username)
			.add("password", password)
			.build();
		return new Request.Builder()
			.url("https://www.boardgamegeek.com/login")
			.post(formBody)
			.build();
	}
}
