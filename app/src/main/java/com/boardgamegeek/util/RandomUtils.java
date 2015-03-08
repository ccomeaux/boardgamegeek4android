package com.boardgamegeek.util;

import java.util.Random;

/**
 * A random singleton.
 */
public class RandomUtils {
	private static Random mRandom;

	public static Random getRandom() {
		if (mRandom == null) {
			mRandom = new Random();
		}
		return mRandom;
	}
}
