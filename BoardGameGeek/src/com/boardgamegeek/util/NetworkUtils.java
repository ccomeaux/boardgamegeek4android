package com.boardgamegeek.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;

public class NetworkUtils {

	public static boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	public static boolean isCharging(Context context) {
		Intent batteryStatus = getBatteryStatus(context);
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
	}

	public static boolean isOnWiFi(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
	}

	public static float getBatteryLevel(Context context) {
		Intent batteryStatus = getBatteryStatus(context);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		return (level / (float) scale);
	}

	public static boolean isBatteryLow(Context context) {
		return getBatteryLevel(context) < 0.15;
	}

	private static Intent getBatteryStatus(Context context) {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, ifilter);
		return batteryStatus;
	}
}
