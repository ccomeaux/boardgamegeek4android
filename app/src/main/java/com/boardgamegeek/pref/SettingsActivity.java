package com.boardgamegeek.pref;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.VersionUtils;
import com.mikepenz.aboutlibraries.Libs;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings("deprecation")
public class SettingsActivity extends AppCompatPreferenceActivity {
	private final static String ACTION_SEARCH = "com.boardgamegeek.prefs.SEARCH";
	private final static String ACTION_LOG = "com.boardgamegeek.prefs.LOG";
	private final static String ACTION_SYNC = "com.boardgamegeek.prefs.SYNC";
	private final static String ACTION_ADVANCED = "com.boardgamegeek.prefs.ADVANCED";
	private final static String ACTION_ABOUT = "com.boardgamegeek.prefs.ABOUT";
	private static final HashMap<String, Integer> mFragmentMap = buildFragmentMap();

	private static HashMap<String, Integer> buildFragmentMap() {
		HashMap<String, Integer> map = new HashMap<>();
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
		buildLegacyPreferences();
	}

	private void buildLegacyPreferences() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
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
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onBuildHeaders(List<Header> target) {
		super.onBuildHeaders(target);
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
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

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return fragmentName.equals(PrefFragment.class.getName());
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class PrefFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
		private int mSyncType = 0;

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

			Preference oslPref = findPreference("open_source_licenses");
			if (oslPref != null) {
				oslPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						new Libs.Builder()
							.withFields(R.string.class.getFields())
							.withLibraries(
								"OkHttp",
								"DragSortListView",
								"Hugo",
								"PhotoView",
								"RangeSeekBar",
								"StickyListHeaders",
								"AndroidIcons",
								"EventBus")
							.withAutoDetect(true)
							.withLicenseShown(true)
							.withActivityTitle(getString(R.string.pref_about_licenses))
							.withActivityTheme(R.style.Theme_bgglight)
							.withAboutVersionShown(true)
							.start(PrefFragment.this.getActivity());
						return true;
					}
				});
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
		public void onStop() {
			super.onStop();
			if (mSyncType > 0) {
				SyncService.sync(getActivity(), mSyncType);
			}
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			switch (key) {
				case "syncStatuses":
					SyncService.clearCollection(getActivity());
					mSyncType |= SyncService.FLAG_SYNC_COLLECTION;
					break;
				case "syncPlays":
					SyncService.clearPlays(getActivity());
					mSyncType |= SyncService.FLAG_SYNC_PLAYS;
					break;
				case "syncBuddies":
					SyncService.clearBuddies(getActivity());
					mSyncType |= SyncService.FLAG_SYNC_BUDDIES;
					break;
			}
		}
	}
}