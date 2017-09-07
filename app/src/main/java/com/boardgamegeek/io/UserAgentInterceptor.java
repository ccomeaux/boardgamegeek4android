package com.boardgamegeek.io;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.util.HelpUtils;

import java.io.IOException;

import hugo.weaving.DebugLog;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {
	private final Context context;

	public UserAgentInterceptor(Context context) {
		this.context = context;
	}

	@DebugLog
	@Override
	public Response intercept(@NonNull Chain chain) throws IOException {
		Request originalRequest = chain.request();
		Request request = originalRequest.newBuilder()
			.header("User-Agent", constructUserAgent())
			.build();
		return chain.proceed(request);
	}

	@DebugLog
	private String constructUserAgent() {
		String userAgent = "BGG4Android";
		if (context != null) {
			userAgent += "/" + HelpUtils.getVersionName(context);
		}
		return userAgent;
	}
}
