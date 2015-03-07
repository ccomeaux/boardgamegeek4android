package com.boardgamegeek.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

/**
 * Helps determine which version is currently running on the device.
 */
public class VersionUtils {
	private VersionUtils() {
	}

	// Can use static final constants like FROYO, declared in later versions
	// of the OS since they are inlined at compile time. This is guaranteed behavior.

	public static boolean isGoogleTV(Context context) {
		return context.getPackageManager().hasSystemFeature("com.google.android.tv");
	}

	public static boolean hasFroyo() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	public static boolean hasGingerbread() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	public static boolean hasHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean hasHoneycombMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
	}

	public static boolean hasHoneycombMR2() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2;
	}

	public static boolean hasICS() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
	}

	public static boolean hasJellyBean() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}

	public static boolean hasJellyBeanMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
	}

	public static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	public static boolean isHoneycombTablet(Context context) {
		return hasHoneycomb() && isTablet(context);
	}
}
