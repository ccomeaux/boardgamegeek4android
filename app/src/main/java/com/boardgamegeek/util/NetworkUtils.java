package com.boardgamegeek.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Static methods to work with the network.
 */
public class NetworkUtils {
	private NetworkUtils() {
	}

	public static boolean isOffline(Context context) {
		if (context == null) return true;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) return true;
		final NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
		return activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting();
	}

	public static boolean isOnWiFi(Context context) {
		if (context == null) return false;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) return false;
		final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
	}
}
