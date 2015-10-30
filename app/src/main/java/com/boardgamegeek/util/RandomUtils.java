package com.boardgamegeek.util;

import java.util.Random;

/**
 * A random singleton.
 */
public class RandomUtils {
	private static Random random;

	public static Random getRandom() {
		if (random == null) {
			random = new Random();
		}
		return random;
	}
}
