package com.boardgamegeek;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BggApplication extends Application {

	private static BggApplication singleton;

	public static BggApplication getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
	}

	public String getUserName() {
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return preferences.getString("username", "");
	}

	public String getPassword() {
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return preferences.getString("password", "");
	}

	public boolean getImageLoad() {
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return preferences.getBoolean("imageLoad", true);
	}

	public boolean getExactSearch() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return preferences.getBoolean("exactSearch", true);
	}

	public boolean getSkipResults() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		return preferences.getBoolean("skipResults", true);
	}

}
