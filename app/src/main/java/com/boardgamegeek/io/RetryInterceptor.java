package com.boardgamegeek.io;


import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import timber.log.Timber;

public class RetryInterceptor implements Interceptor {
	private static final int COLLECTION_REQUEST_PROCESSING = 202;
	private static final int API_RATE_EXCEEDED = 503;

	private static final int BACKOFF_TYPE_EXPONENTIAL = 0;
	private static final int BACKOFF_TYPE_GEOMETRIC = 1;

	private static final long MIN_WAIT_TIME = 1000L;
	private static final long MAX_WAIT_TIME = 60000L;
	private static final int MAX_RETRIES = 4;

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();
		int numberOfRetries = 0;
		int responseCode;
		do {
			Response response = chain.proceed(originalRequest);
			responseCode = response.code();
			if (responseCode == COLLECTION_REQUEST_PROCESSING ||
				responseCode == API_RATE_EXCEEDED) {
				Timber.i("Retry-able response code %s", responseCode);
				numberOfRetries++;
				wait(numberOfRetries);
				Timber.i("...retrying");
			} else {
				return response;
			}
		} while (numberOfRetries < getMaxRetries(responseCode));
		Timber.w("Exceeded maximum number of retries of %,d.", getMaxRetries(responseCode));
		return chain.proceed(originalRequest);
	}

	private void wait(int numberOfRetries) {
		long waitTime = calculateWaitTime(numberOfRetries);
		Timber.i("...retry #%1$,d in %2$,dms...", numberOfRetries, waitTime);
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			Timber.w(e, "Interrupted while sleeping during retry.");
		}
	}

	private long calculateWaitTime(int numberOfRetries) {
		long waitTime = getMinWaitTime();
		switch (getBackOffType()) {
			case BACKOFF_TYPE_EXPONENTIAL:
				waitTime = ((long) Math.pow(2, numberOfRetries - 1) * getMinWaitTime());
				break;
			case BACKOFF_TYPE_GEOMETRIC:
				waitTime = numberOfRetries * numberOfRetries * getMinWaitTime();
				break;
		}
		return Math.min(getMaxWaitTime(), waitTime);
	}

	@SuppressWarnings("SameReturnValue")
	private int getBackOffType() {
		return BACKOFF_TYPE_EXPONENTIAL;
	}

	protected long getMinWaitTime() {
		return MIN_WAIT_TIME;
	}

	protected long getMaxWaitTime() {
		return MAX_WAIT_TIME;
	}

	protected int getMaxRetries(int responseCode) {
		if (responseCode == COLLECTION_REQUEST_PROCESSING) {
			return 8;
		}
		if (responseCode == API_RATE_EXCEEDED) {
			return 2;
		}
		return MAX_RETRIES;
	}
}
