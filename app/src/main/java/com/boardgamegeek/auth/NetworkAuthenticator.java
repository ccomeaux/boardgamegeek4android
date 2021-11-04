package com.boardgamegeek.auth;

import com.boardgamegeek.util.HttpUtils;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class NetworkAuthenticator {
	private static final boolean MOCK_LOGIN = false;

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
	private static BggCookieJar performAuthenticate(@NonNull String username, @NonNull String password, @NonNull String method) throws IOException {
		final BggCookieJar cookieJar = new BggCookieJar();
		final OkHttpClient client = HttpUtils.getHttpClient(false).newBuilder()
			.cookieJar(cookieJar)
			.build();
		Request post = buildRequest(username, password);
		final Response response = client.newCall(post).execute();
		if (response.isSuccessful()) {
			if (cookieJar.isValid()) {
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
	}

	@NonNull
	private static Request buildRequest(@NonNull String username, @NonNull String password) {
		JsonObject credentials = new JsonObject();
		credentials.addProperty("username", username);
		credentials.addProperty("password", password);
		JsonObject body = new JsonObject();
		body.add("credentials", credentials);
		byte[] bytes;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			bytes = body.toString().getBytes(StandardCharsets.UTF_8);
		} else {
			//noinspection CharsetObjectCanBeUsed
			bytes = body.toString().getBytes(Charset.forName("UTF-8"));
		}
		return new Builder()
			.url("https://boardgamegeek.com/login/api/v1")
			.post(RequestBody.create(bytes))
			.addHeader("Content-Type", "application/json")
			.build();
	}
}
