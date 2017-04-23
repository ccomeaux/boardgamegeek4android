package com.boardgamegeek.io;

public class FixedBackOff implements BackOff {
	public static final int DEFAULT_INTERVAL_MILLIS = 5000;
	public static final int DEFAULT_MAX_BACKOFF_COUNT = 1;

	private final int intervalMillis;
	private final int maxBackOffCount;

	private int backOffCount;

	protected FixedBackOff(Builder builder) {
		intervalMillis = builder.intervalMillis;
		maxBackOffCount = builder.maxBackOffCount;
		checkArgument(intervalMillis > 0);
		checkArgument(maxBackOffCount >= 0);
		reset();
	}

	@Override
	public long nextBackOffMillis() {
		backOffCount++;
		if (backOffCount > maxBackOffCount) return BackOff.STOP;
		return intervalMillis;
	}

	@Override
	public void reset() {
		backOffCount = 0;
	}

	private static void checkArgument(boolean expression) {
		if (!expression) {
			throw new IllegalArgumentException();
		}
	}

	public static class Builder {
		int intervalMillis = DEFAULT_INTERVAL_MILLIS;
		int maxBackOffCount = DEFAULT_MAX_BACKOFF_COUNT;

		public Builder() {
		}

		public FixedBackOff build() {
			return new FixedBackOff(this);
		}

		public Builder setIntervalMillis(int intervalMillis) {
			this.intervalMillis = intervalMillis;
			return this;
		}

		public Builder setMaxBackOffCount(int maxBackOffCount) {
			this.maxBackOffCount = maxBackOffCount;
			return this;
		}
	}
}
