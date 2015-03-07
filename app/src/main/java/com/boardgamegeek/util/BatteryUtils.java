package com.boardgamegeek.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * Static methods for inspecting the battery information.
 */
public class BatteryUtils {
	private BatteryUtils() {
	}

	public static boolean isCharging(Context context) {
		Intent batteryStatus = getBatteryStatus(context);
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
	}

	public static float getBatteryLevel(Context context) {
		Intent batteryStatus = getBatteryStatus(context);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		return (level / (float) scale);
	}

	public static boolean isBatteryLow(Context context) {
		return getBatteryLevel(context) < 0.15; // 15% matches system low battery level
	}

	private static Intent getBatteryStatus(Context context) {
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		return context.registerReceiver(null, filter);
	}
}
