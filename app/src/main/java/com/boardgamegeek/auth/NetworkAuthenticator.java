package com.boardgamegeek.auth;

import com.boardgamegeek.util.HttpUtils;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

import timber.log.Timber;

public class NetworkAuthenticator {
	@SuppressWarnings("FieldCanBeLocal") private static final boolean MOCK_LOGIN = false;

	private NetworkAuthenticator() {
	}

	/**
	 * Authenticates to BGG with the specified username and password, returning the cookie store to use on subsequent
	 * requests, or null if authentication fails.
	 */
	public static AuthResponse authenticate(String username, String password) {
		if (MOCK_LOGIN) {
			return AuthResponse.getMock();
		}

		String uri = "https://www.boardgamegeek.com/login";

		final Response resp;
		RequestBody formBody = new FormEncodingBuilder()
			.add("username", username)
			.add("password", password)
			.build();

		Timber.i("Authenticating to: " + uri);

		Request post = new Request.Builder()
			.url(uri)
			.post(formBody)
			.build();

		try {
			final OkHttpClient client = HttpUtils.getHttpClient();

			CookieManager cm = new CookieManager();
			cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			client.setCookieHandler(cm);

			resp = client.newCall(post).execute();
			Timber.d(resp.toString());

			if (resp.isSuccessful()) {
				Timber.w("Successful authentication");
				return AuthResponse.fromCookieStore(cm.getCookieStore());
			} else {
				Timber.w("Bad response code - " + resp.code());
			}
		} catch (final IOException e) {
			Timber.w(e, "IOException when attempting to authenticate");
		} finally {
			Timber.w("Authentication complete");
		}
		return null;
	}
}
