package com.boardgamegeek.io;

public class ExponentialBackOff implements BackOff {
	public static final int DEFAULT_INITIAL_INTERVAL_MILLIS = 500;
	public static final double DEFAULT_RANDOMIZATION_FACTOR = 0.5;
	public static final double DEFAULT_MULTIPLIER = 1.5;
	public static final int DEFAULT_MAX_INTERVAL_MILLIS = 60000;
	public static final int DEFAULT_MAX_ELAPSED_TIME_MILLIS = 900000;

	private final int initialIntervalMillis;
	private final double randomizationFactor;
	private final double multiplier;
	private final int maxIntervalMillis;
	private final int maxElapsedTimeMillis;
	private int currentIntervalMillis;
	private long startTimeNanos;

	public ExponentialBackOff() {
		this(new Builder());
	}

	protected ExponentialBackOff(Builder builder) {
		initialIntervalMillis = builder.initialIntervalMillis;
		randomizationFactor = builder.randomizationFactor;
		multiplier = builder.multiplier;
		maxIntervalMillis = builder.maxIntervalMillis;
		maxElapsedTimeMillis = builder.maxElapsedTimeMillis;
		checkArgument(initialIntervalMillis > 0);
		checkArgument(0 <= randomizationFactor && randomizationFactor < 1);
		checkArgument(multiplier >= 1);
		checkArgument(maxIntervalMillis >= initialIntervalMillis);
		checkArgument(maxElapsedTimeMillis > 0);
		reset();
	}

	public final void reset() {
		currentIntervalMillis = initialIntervalMillis;
		startTimeNanos = System.nanoTime();
	}

	public long nextBackOffMillis() {
		if (getElapsedTimeMillis() > maxElapsedTimeMillis) {
			return STOP;
		}
		int randomizedInterval = getRandomValueFromInterval(randomizationFactor, Math.random(), currentIntervalMillis);
		incrementCurrentInterval();
		return randomizedInterval;
	}

	static int getRandomValueFromInterval(double randomizationFactor, double random, int currentIntervalMillis) {
		double delta = randomizationFactor * currentIntervalMillis;
		double minInterval = currentIntervalMillis - delta;
		double maxInterval = currentIntervalMillis + delta;
		return (int) (minInterval + (random * (maxInterval - minInterval + 1)));
	}

	public final long getElapsedTimeMillis() {
		return (System.nanoTime() - startTimeNanos) / 1000000;
	}

	private void incrementCurrentInterval() {
		// Check for overflow, if overflow is detected set the current interval to the max interval.
		if (currentIntervalMillis >= maxIntervalMillis / multiplier) {
			currentIntervalMillis = maxIntervalMillis;
		} else {
			currentIntervalMillis *= multiplier;
		}
	}

	private static void checkArgument(boolean expression) {
		if (!expression) {
			throw new IllegalArgumentException();
		}
	}

	public static class Builder {
		int initialIntervalMillis = DEFAULT_INITIAL_INTERVAL_MILLIS;
		double randomizationFactor = DEFAULT_RANDOMIZATION_FACTOR;
		double multiplier = DEFAULT_MULTIPLIER;
		int maxIntervalMillis = DEFAULT_MAX_INTERVAL_MILLIS;
		int maxElapsedTimeMillis = DEFAULT_MAX_ELAPSED_TIME_MILLIS;

		public Builder() {
		}

		public ExponentialBackOff build() {
			return new ExponentialBackOff(this);
		}

		public Builder setInitialIntervalMillis(int initialIntervalMillis) {
			this.initialIntervalMillis = initialIntervalMillis;
			return this;
		}

		public Builder setRandomizationFactor(double randomizationFactor) {
			this.randomizationFactor = randomizationFactor;
			return this;
		}

		public Builder setMultiplier(double multiplier) {
			this.multiplier = multiplier;
			return this;
		}

		public Builder setMaxIntervalMillis(int maxIntervalMillis) {
			this.maxIntervalMillis = maxIntervalMillis;
			return this;
		}

		public Builder setMaxElapsedTimeMillis(int maxElapsedTimeMillis) {
			this.maxElapsedTimeMillis = maxElapsedTimeMillis;
			return this;
		}
	}
}
