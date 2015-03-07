package com.boardgamegeek.util;

import java.util.Random;

/**
 * A random singleton.
 */
public class RandomUtil {
	private static Random mRandom;

	public static Random getRandom() {
		if (mRandom == null) {
			mRandom = new Random();
		}
		return mRandom;
	}
}
