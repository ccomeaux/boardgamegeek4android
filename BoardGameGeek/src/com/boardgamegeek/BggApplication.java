package com.boardgamegeek;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BggApplication extends Application {
	public final static String siteUrl = "http://www.boardgamegeek.com/";
	public static final String HELP_COLLECTION_KEY = "help.collection";
	public static final String HELP_SEARCHRESULTS_KEY = "help.searchresults";
	public static final String HELP_LOGPLAY_KEY = "help.logplay";
	public static final String HELP_COLORS_KEY = "help.colors";

	public static final String AUTHTOKEN_TYPE = "com.boardgamegeek";
	public static final String ACCOUNT_TYPE = "com.boardgamegeek";

	private static BggApplication singleton;

	public static BggApplication getInstance() {
		return singleton;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
	}

	public boolean showHelp(String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final int shownVersion = preferences.getInt(key, 0);
		return version > shownVersion;
	}

	public boolean updateHelp(String key, int version) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		return preferences.edit().putInt(key, version).commit();
	}
}
