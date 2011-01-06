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
	
	public String getUserName(){
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.getString("username", "");
	}
}
