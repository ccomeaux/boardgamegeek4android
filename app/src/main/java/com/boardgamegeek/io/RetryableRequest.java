package com.boardgamegeek.io;

import timber.log.Timber;

public abstract class RetryableRequest<T> {
	private static final int BACKOFF_TYPE_EXPONENTIAL = 0;
	private static final int BACKOFF_TYPE_GEOMETRIC = 1;

	private static final long MIN_WAIT_TIME = 100L;
	private static final long MAX_WAIT_TIME = 60000L;
	private static final int MAX_RETRIES = 10;
	protected final BggService bggService;

	public RetryableRequest(BggService service) {
		bggService = service;
	}

	public T execute() {
		int numberOfRetries = 0;
		do {
			try {
				return request();
			} catch (Exception e) {
				if (e.getCause() instanceof RetryableException) {
					Timber.w(e, "Retry-able exception");
					numberOfRetries++;
					wait(numberOfRetries);
				} else {
					Timber.e(e, "Non-retry-able exception");
					throw e;
				}
			}
		} while (numberOfRetries < getMaxRetries());
		String errorMessage = String.format("Exceeded maximum number of retries: [%s]", getMaxRetries());
		Timber.w(errorMessage);
		throw new RuntimeException(errorMessage);
	}

	protected abstract T request();

	private void wait(int numberOfRetries) {
		long waitTime = calculateWaitTime(numberOfRetries);
		Timber.i("...retry #" + numberOfRetries + " in " + waitTime + "ms");
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			Timber.e(e, "Interrupted while sleeping during retry.");
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

	protected int getMaxRetries() {
		return MAX_RETRIES;
	}
}
