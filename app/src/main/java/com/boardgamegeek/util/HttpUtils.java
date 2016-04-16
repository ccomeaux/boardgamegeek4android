package com.boardgamegeek.util;

import android.net.Uri;
import android.text.TextUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class HttpUtils {
	private static final int HTTP_REQUEST_TIMEOUT_SEC = 15;

	private HttpUtils() {
	}

	/**
	 * Encodes {@code s} using UTF-8 using the format required by {@code application/x-www-form-urlencoded} MIME content type.
	 */
	public static String encode(String s) {
		return Uri.encode(s, "UTF-8");
	}

	/**
	 * Configures the default HTTP client.
	 */
	public static OkHttpClient getHttpClient() {
		OkHttpClient.Builder builder = new Builder();
		OkHttpClient client = builder
			.connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.build();
		return client;
	}

	/**
	 * Ensures the URL has a scheme, setting it to HTTPS if missing.
	 */
	public static String ensureScheme(String url) {
		if (TextUtils.isEmpty(url)) {
			return url;
		}
		if (url.startsWith("//")) {
			return "https:" + url;
		}
		return url;
	}
}
