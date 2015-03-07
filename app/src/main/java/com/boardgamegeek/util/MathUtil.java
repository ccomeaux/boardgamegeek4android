package com.boardgamegeek.util;

public class MathUtil {
	private MathUtil() {
	}

	public static int constrain(int number, int min, int max) {
		return Math.max(min, Math.min(max, number));
	}

	public static float constrain(float number, float min, float max) {
		return Math.max(min, Math.min(max, number));
	}
}
