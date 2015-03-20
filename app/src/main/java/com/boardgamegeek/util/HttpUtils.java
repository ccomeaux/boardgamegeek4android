package com.boardgamegeek.util;

import android.text.TextUtils;

import com.squareup.okhttp.OkHttpClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class HttpUtils {
	private static final int HTTP_REQUEST_TIMEOUT_SEC = 15;

	private HttpUtils() {
	}

	/**
	 * Encodes {@code s} using UTF-8 using the format required by {@code application/x-www-form-urlencoded} MIME content type.
	 */
	public static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Timber.e(e, "What do you mean UTF-8 isn't supported?!");
		}
		return s;
	}

	/**
	 * Configures the default HTTP client.
	 */
	public static OkHttpClient getHttpClient() {
		OkHttpClient client = new OkHttpClient();
		client.setConnectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
		client.setReadTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
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
