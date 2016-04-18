package com.boardgamegeek.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.io.AuthInterceptor;
import com.boardgamegeek.io.RetryInterceptor;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
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

	public static OkHttpClient getHttpClient() {
		Builder builder = getBuilder();
		builder.interceptors().add(new RetryInterceptor());
		return builder.build();
	}

	public static OkHttpClient getHttpClientWithAuth(Context context) {
		OkHttpClient.Builder builder = getBuilder();
		builder.addInterceptor(new AuthInterceptor(context));
		builder.addInterceptor(new RetryInterceptor());
		return builder.build();
	}

	public static OkHttpClient getHttpClientWithCache(Context context) {
		OkHttpClient.Builder builder = getBuilder();
		File cacheDir = new File(context.getCacheDir(), "http");
		Cache cache = new Cache(cacheDir, 10 * 1024 * 1024);
		return builder.cache(cache).build();
	}

	@NonNull
	private static Builder getBuilder() {
		Builder builder = new Builder()
			.connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
		if (BuildConfig.DEBUG) {
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
