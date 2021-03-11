package com.boardgamegeek.util;

import android.os.SystemClock;
import android.widget.Chronometer;

/**
 * Various static methods for use on views and fragments.
 */
public class UIUtils {
	public static void startTimerWithSystemTime(Chronometer timer, long time) {
		timer.setBase(time - System.currentTimeMillis() + SystemClock.elapsedRealtime());
		timer.start();
	}
}
