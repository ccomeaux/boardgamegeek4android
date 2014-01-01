package com.boardgamegeek.pref;

import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.VersionUtils;

@SuppressWarnings("deprecation")
public class Preferences extends PreferenceActivity {
	private final static String ACTION_SEARCH = "com.boardgamegeek.prefs.SEARCH";
	private final static String ACTION_LOG = "com.boardgamegeek.prefs.LOG";
	private final static String ACTION_SYNC = "com.boardgamegeek.prefs.SYNC";
	private final static String ACTION_ADVANCED = "com.boardgamegeek.prefs.ADVANCED";
	private final static String ACTION_ABOUT = "com.boardgamegeek.prefs.ABOUT";
	private static final HashMap<String, Integer> mFragmentMap = buildFragmentMap();

	private static HashMap<String, Integer> buildFragmentMap() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		map.put(ACTION_SEARCH, R.xml.preference_search);
		map.put(ACTION_LOG, R.xml.preference_log);
		map.put(ACTION_SYNC, R.xml.preference_sync);
		map.put(ACTION_ADVANCED, R.xml.preference_advanced);
		map.put(ACTION_ABOUT, R.xml.preference_about);
		return map;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String action = getIntent().getAction();
		if (action != null) {
			Integer fragmentId = mFragmentMap.get(action);
			if (fragmentId != null) {
				addPreferencesFromResource(fragmentId);
			}
		} else if (!VersionUtils.hasHoneycomb()) {
			addPreferencesFromResource(R.xml.preference_headers_legacy);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	@Deprecated
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
		// The following code make sub screens appear in the light theme, otherwise it displays black on black
		if (preference != null)
			if (preference instanceof PreferenceScreen)
				if (((PreferenceScreen) preference).getDialog() != null)
					((PreferenceScreen) preference)
						.getDialog()
						.getWindow()
						.getDecorView()
						.setBackgroundDrawable(
							this.getWindow().getDecorView().getBackground().getConstantState().newDrawable());
		return false;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class PrefFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			String fragment = getArguments().getString("fragment");
			if (fragment != null) {
				Integer fragmentId = mFragmentMap.get(fragment);
				if (fragmentId != null) {
					addPreferencesFromResource(fragmentId);
				}
			}
		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onPause() {
			super.onPause();
			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if ("syncStatuses".equals(key)) {
				SyncService.clearCollection(getActivity());
			}
		}
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return "com.boardgamegeek.pref.Preferences$PrefFragment".equals(fragmentName);
	}
}