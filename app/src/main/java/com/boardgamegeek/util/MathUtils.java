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
}
