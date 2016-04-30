package com.boardgamegeek.io;

import android.content.Context;

import com.boardgamegeek.util.HelpUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {
	private final Context context;

	public UserAgentInterceptor(Context context) {
		this.context = context;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();
		Request request = originalRequest.newBuilder()
			.header("User-Agent", constructUserAgent())
			.build();
		return chain.proceed(request);
	}

	private String constructUserAgent() {
		String userAgent = "BGG4Android";
		if (context != null) {
			userAgent += "/" + HelpUtils.getVersionName(context);
		}
		return userAgent;
	}
}
