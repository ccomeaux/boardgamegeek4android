package com.boardgamegeek.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.io.AuthInterceptor;
import com.boardgamegeek.io.RetryInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.logging.HttpLoggingInterceptor;

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
		return getHttpClient(false);
	}

	/**
	 * Configures the default HTTP client, optionally with a debug logger.
	 */
	public static OkHttpClient getHttpClient(boolean includeDebug) {
		Builder builder = getBuilder(includeDebug);
		builder.interceptors().add(new RetryInterceptor());
		return builder.build();
	}

	/**
	 * Configures the default HTTP client, adds BGG auth, optionally with a debug logger.
	 */
	public static OkHttpClient getHttpClientWithAuth(boolean includeDebug, Context context) {
		OkHttpClient.Builder builder = getBuilder(includeDebug);
		builder.addInterceptor(new AuthInterceptor(context));
		builder.addInterceptor(new RetryInterceptor());
		return builder.build();
	}

	@NonNull
	private static Builder getBuilder(boolean includeDebug) {
		Builder builder = new Builder()
			.connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
		if (includeDebug) {
			HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
			httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
			builder.addInterceptor(httpLoggingInterceptor);
		}
		return builder;
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
