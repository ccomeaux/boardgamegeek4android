package com.boardgamegeek;

import com.boardgamegeek.pref.ListPreferenceMultiSelect;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

public class BggApplication extends Application {
	private final static String TAG = "BggApplication";
	public final static String siteUrl = "http://www.boardgamegeek.com/";

	private static BggApplication singleton;

	public static BggApplication getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
	}

	public static String getVersionDescription(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			return "Version " + pInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "NameNotFoundException in getVersion", e);
		}
		return "";
	}


	public String getUserName() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getString("username", "");
	}

	public String getPassword() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getString("password", "");
	}

	public boolean getImageLoad() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("imageLoad", true);
	}

	public boolean getExactSearch() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("exactSearch", true);
	}

	public boolean getSkipResults() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("skipResults", true);
	}

	public String[] getSyncStatuses() {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String statuses = preferences.getString("syncStatuses", "");
		return ListPreferenceMultiSelect.parseStoredValue(statuses);
	}

	public boolean getSyncBuddies(){
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getBoolean("syncBuddies", true);
	}
}
