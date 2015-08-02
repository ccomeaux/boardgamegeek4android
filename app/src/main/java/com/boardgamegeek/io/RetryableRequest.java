package com.boardgamegeek.io;

import timber.log.Timber;

public abstract class RetryableRequest<T> {

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
		long waitTime = ((long) Math.pow(2, retryCount) * getMinWaitTime());
		return Math.min(getMaxWaitTime(), waitTime);
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
