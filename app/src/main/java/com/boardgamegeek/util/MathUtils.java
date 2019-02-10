package com.boardgamegeek.util;

public class MathUtils {
	private MathUtils() {
	}

	public static int constrain(int number, int min, int max) {
		return Math.max(min, Math.min(max, number));
	}

	public static double cdf(double value, double lambda) {
		return 1.0 - Math.exp(-1 * lambda * value);
	}

	public static double invcdf(double value, double lambda) {
		return -Math.log(1.0 - value) / lambda;
	}
}
