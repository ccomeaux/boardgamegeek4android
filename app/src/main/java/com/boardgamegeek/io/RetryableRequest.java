package com.boardgamegeek.io;

import timber.log.Timber;

public abstract class RetryableRequest<T> {
	public static final int BACKOFF_TYPE_EXPONENTIAL = 0;
	public static final int BACKOFF_TYPE_GEOMETRIC = 1;

	private static final long MIN_WAIT_TIME = 100L;
	private static final long MAX_WAIT_TIME = 60000L;
	private static final int MAX_RETRIES = 10;
	protected final BggService mService;

	public RetryableRequest(BggService service) {
		mService = service;
	}

	public T execute() {
		int retries = 0;
		do {
			try {
				return request();
			} catch (Exception e) {
				if (e.getCause() instanceof RetryableException) {
					Timber.w(e, "Retryable exception");
					retries++;
					long waitTime = getWaitTime(retries);
					Timber.i("...retry #" + retries + " in " + waitTime + "ms");
					try {
						Thread.sleep(waitTime);
					} catch (InterruptedException e1) {
						Timber.e(e1, "Interrupted while sleeping during retry.");
					}
				} else {
					Timber.e(e, "Syncing plays");
					throw e;
				}
			}
		} while (retries < getMaxRetries());
		Timber.w("Exceeded maximum retries");
		throw new RuntimeException("Can't sync plays due to service error.");
	}

	protected abstract T request();

	protected long getWaitTime(int retryCount) {
		long waitTime = getMinWaitTime();
		switch (getBackOffType()) {
			case BACKOFF_TYPE_EXPONENTIAL:
				waitTime = ((long) Math.pow(2, retryCount - 1) * getMinWaitTime());
				break;
			case BACKOFF_TYPE_GEOMETRIC:
				waitTime = retryCount * retryCount * getMinWaitTime();
				break;
		}
		return Math.min(getMaxWaitTime(), waitTime);
	}

	private int getBackOffType() {
		return BACKOFF_TYPE_EXPONENTIAL;
	}

	protected long getMinWaitTime() {
		return MIN_WAIT_TIME;
	}

	protected long getMaxWaitTime() {
		return MAX_WAIT_TIME;
	}

	protected int getMaxRetries() {
		return MAX_RETRIES;
	}
}
