package com.boardgamegeek.util;

import android.content.Context;
import android.net.Uri;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.io.AuthInterceptor;
import com.boardgamegeek.io.RetryInterceptor;
import com.boardgamegeek.io.UserAgentInterceptor;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.Cache;
import okhttp3.Interceptor;
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
		final List<Interceptor> interceptors = builder.interceptors();
		interceptors.add(new UserAgentInterceptor(null));
		interceptors.add(new RetryInterceptor());
		addLoggingInterceptor(builder);
		return builder.build();
	}

	public static OkHttpClient getHttpClientWithAuth(Context context) {
		OkHttpClient.Builder builder = getBuilder();
		builder.addInterceptor(new UserAgentInterceptor(context));
		builder.addInterceptor(new AuthInterceptor(context));
		builder.addInterceptor(new RetryInterceptor());
		addLoggingInterceptor(builder);
		return builder.build();
	}

	public static OkHttpClient getHttpClientWithCache(Context context) {
		OkHttpClient.Builder builder = getBuilder();
		final List<Interceptor> interceptors = builder.interceptors();
		interceptors.add(new UserAgentInterceptor(context));
		addLoggingInterceptor(builder);
		File cacheDir = new File(context.getCacheDir(), "http");
		Cache cache = new Cache(cacheDir, 10 * 1024 * 1024);
		return builder.cache(cache).build();
	}

	@NonNull
	private static Builder getBuilder() {
		return new Builder()
			.connectTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.readTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS)
			.writeTimeout(HTTP_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
	}

	private static void addLoggingInterceptor(Builder builder) {
		if (BuildConfig.DEBUG) {
			HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
			httpLoggingInterceptor.level(HttpLoggingInterceptor.Level.BODY);
			builder.addInterceptor(httpLoggingInterceptor);
			builder.addNetworkInterceptor(new StethoInterceptor());
		}
	}
}
