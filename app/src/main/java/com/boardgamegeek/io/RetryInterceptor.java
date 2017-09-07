package com.boardgamegeek.io;


import android.support.annotation.NonNull;

import com.boardgamegeek.io.ExponentialBackOff.Builder;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class RetryInterceptor implements Interceptor {
	private static final int COLLECTION_REQUEST_PROCESSING = 202;
	private static final int API_RATE_EXCEEDED = 503;

	private final BackOff backOff202;
	private final BackOff backOff503;

	public RetryInterceptor() {
		super();
		backOff202 = new Builder()
			.setInitialIntervalMillis(1500)
			.setMultiplier(2.5)
			.setRandomizationFactor(0.30)
			.setMaxIntervalMillis(60000)
			.setMaxElapsedTimeMillis(300000)
			.build();
		backOff503 = new FixedBackOff.Builder()
			.setIntervalMillis(5000)
			.setMaxBackOffCount(1)
			.build();
	}

	@Override
	public Response intercept(@NonNull Chain chain) throws IOException {
		resetBackOff();
		Request originalRequest = chain.request();
		Response response = chain.proceed(originalRequest);
		long millis = nextBackOffMillis(response);
		while (millis != BackOff.STOP) {
			Timber.d("...sleeping for %,d ms", millis);
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				Timber.w(e, "Interrupted while sleeping during retry.");
				return response;
			}
			Timber.d("...retrying");
			response = chain.proceed(originalRequest);
			millis = nextBackOffMillis(response);
		}
		return response;
	}

	private void resetBackOff() {
		backOff202.reset();
		backOff503.reset();
	}

	private long nextBackOffMillis(Response response) {
		if (response.code() == COLLECTION_REQUEST_PROCESSING) {
			return backOff202.nextBackOffMillis();
		} else if (response.code() == API_RATE_EXCEEDED) {
			return backOff503.nextBackOffMillis();
		}
		return BackOff.STOP;
	}
}
