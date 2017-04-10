package com.boardgamegeek.util;

import java.math.BigDecimal;
import java.math.MathContext;

public class MathUtils {
	private MathUtils() {
	}

	public static int constrain(int number, int min, int max) {
		return Math.max(min, Math.min(max, number));
	}

	public static float constrain(float number, float min, float max) {
		return Math.max(min, Math.min(max, number));
	}

	public static double constrain(double number, double min, double max) {
		return Math.max(min, Math.min(max, number));
	}

	public static int significantDigits(int number, int digits) {
		BigDecimal bd = new BigDecimal(number);
		bd = bd.round(new MathContext(digits));
		return bd.intValue();
	}

	public static double cdf(double value, double lambda) {
		return 1.0 - Math.exp(-1 * lambda * value);
	}

	public static double invcdf(double value, double lambda) {
		return -Math.log(1.0 - value) / lambda;
	}
}
