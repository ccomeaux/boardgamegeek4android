package com.boardgamegeek.auth;

import android.content.Context;
import android.os.Bundle;

import com.boardgamegeek.util.HttpUtils;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;
import com.google.gson.JsonObject;

import java.io.IOException;
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
	public static BggCookieJar authenticate(@NonNull String username, @NonNull String password, @NonNull String method, Context context) {
		if (MOCK_LOGIN) {
			return BggCookieJar.getMock();
		} else {
			return tryAuthenticate(username, password, method, context);
		}
	}

	@Nullable
	private static BggCookieJar tryAuthenticate(@NonNull String username, @NonNull String password, @NonNull String method, Context context) {
		FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
		try {
			return performAuthenticate(username, password, method, firebaseAnalytics);
		} catch (@NonNull final IOException e) {
			logAuthFailure(method, "IOException", firebaseAnalytics);
		} finally {
			Timber.w("Authentication complete");
		}
		return null;
	}

	@Nullable
	private static BggCookieJar performAuthenticate(@NonNull String username, @NonNull String password, @NonNull String method, FirebaseAnalytics firebaseAnalytics) throws IOException {
		final BggCookieJar cookieJar = new BggCookieJar();
		final OkHttpClient client = HttpUtils.getHttpClient(false).newBuilder()
			.cookieJar(cookieJar)
			.build();
		Request post = buildRequest(username, password);
		final Response response = client.newCall(post).execute();
		if (response.isSuccessful()) {
			if (cookieJar.isValid()) {
				Bundle bundle = new Bundle();
				bundle.putString(Param.METHOD, method);
				bundle.putString(Param.SUCCESS, "true");
				firebaseAnalytics.logEvent(Event.LOGIN, bundle);
				return cookieJar;
			} else {
				logAuthFailure(method, "Invalid cookie jar", firebaseAnalytics);
			}
		} else {
			logAuthFailure(method, "Response: " + response.toString(), firebaseAnalytics);
		}
		return null;
	}

	private static void logAuthFailure(String method, String reason, FirebaseAnalytics firebaseAnalytics) {
		Timber.w("Failed %1$s login: %2$s", method, reason);
		Bundle bundle = new Bundle();
		bundle.putString(Param.METHOD, method);
		bundle.putString(Param.SUCCESS, "false");
		bundle.putString("Reason", reason);
		firebaseAnalytics.logEvent(Event.LOGIN, bundle);
	}

	@NonNull
	private static Request buildRequest(@NonNull String username, @NonNull String password) {
		JsonObject credentials = new JsonObject();
		credentials.addProperty("username", username);
		credentials.addProperty("password", password);
		JsonObject body = new JsonObject();
		body.add("credentials", credentials);
		return new Builder()
			.url("https://boardgamegeek.com/login/api/v1")
			.post(RequestBody.create(body.toString().getBytes(StandardCharsets.UTF_8)))
			.addHeader("Content-Type", "application/json")
			.build();
	}
}
