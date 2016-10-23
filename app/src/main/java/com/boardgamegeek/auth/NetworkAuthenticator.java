package com.boardgamegeek.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.boardgamegeek.util.HttpUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LoginEvent;

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
	public static BggCookieJar authenticate(@NonNull String username, @NonNull String password, @NonNull String method) {
		if (MOCK_LOGIN) {
			return BggCookieJar.getMock();
		} else {
			return tryAuthenticate(username, password, method);
		}
	}

	@Nullable
	private static BggCookieJar tryAuthenticate(@NonNull String username, @NonNull String password, @NonNull String method) {
		try {
			return performAuthenticate(username, password, method);
		} catch (@NonNull final IOException e) {
			logAuthFailure(method, "IOException");
		} finally {
			Timber.w("Authentication complete");
		}
		return null;
	}

	@Nullable
	@DebugLog
	private static BggCookieJar performAuthenticate(@NonNull String username, @NonNull String password, @NonNull String method) throws IOException {
		final BggCookieJar cookieJar = new BggCookieJar();
		final OkHttpClient client = HttpUtils.getHttpClient().newBuilder()
			.cookieJar(cookieJar)
			.build();
		Request post = buildRequest(username, password);
		final Response response = client.newCall(post).execute();
		if (response.isSuccessful()) {
			if (cookieJar.isValid()) {
				Answers.getInstance().logLogin(new LoginEvent()
					.putMethod(method)
					.putSuccess(true));
				return cookieJar;
			} else {
				logAuthFailure(method, "Invalid cookie jar");
			}
		} else {
			logAuthFailure(method, "Response: " + response.toString());
		}
		return null;
	}

	private static void logAuthFailure(String method, String reason) {
		Timber.w("Failed %1$s login: %2$s", method, reason);
		Answers.getInstance().logLogin(new LoginEvent()
			.putMethod(method)
			.putSuccess(false)
			.putCustomAttribute("Reason", reason));
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
