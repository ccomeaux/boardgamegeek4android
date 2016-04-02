package com.boardgamegeek.pref;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.util.ArrayMap;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.mikepenz.aboutlibraries.LibsBuilder;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {
	private final static String ACTION_LOG = "com.boardgamegeek.prefs.LOG";
	private final static String ACTION_SYNC = "com.boardgamegeek.prefs.SYNC";
	private final static String ACTION_ADVANCED = "com.boardgamegeek.prefs.ADVANCED";
	private final static String ACTION_ABOUT = "com.boardgamegeek.prefs.ABOUT";
	private static final ArrayMap<String, Integer> FRAGMENT_MAP = buildFragmentMap();

	private static ArrayMap<String, Integer> buildFragmentMap() {
		ArrayMap<String, Integer> map = new ArrayMap<>();
		map.put(ACTION_LOG, R.xml.preference_log);
		map.put(ACTION_SYNC, R.xml.preference_sync);
		map.put(ACTION_ADVANCED, R.xml.preference_advanced);
		map.put(ACTION_ABOUT, R.xml.preference_about);
		return map;
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		super.onBuildHeaders(target);
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return fragmentName.equals(PrefFragment.class.getName());
	}

	public static class PrefFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
		private int syncType = 0;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			String fragment = getArguments().getString("fragment");
			if (fragment != null) {
				Integer fragmentId = FRAGMENT_MAP.get(fragment);
				if (fragmentId != null) {
					addPreferencesFromResource(fragmentId);
				}
			}

			Preference oslPref = findPreference("open_source_licenses");
			if (oslPref != null) {
				oslPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						new LibsBuilder()
							.withFields(R.string.class.getFields())
							.withLibraries(
								"DragSortListView",
								"Hugo",
								"PhotoView",
								"RangeSeekBar",
								"StickyListHeaders",
								"AndroidIcons",
								"MPAndroidChart",
								"AndroidRandomColor",
								"LeakCanary")
							.withAutoDetect(true)
							.withLicenseShown(true)
							.withActivityTitle(getString(R.string.pref_about_licenses))
							.withActivityTheme(R.style.Theme_bgglight_NoActionBar)
							.withAboutIconShown(true)
							.withAboutAppName(getString(R.string.app_name))
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
			if (syncType > 0) {
				SyncService.sync(getActivity(), syncType);
			}
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			switch (key) {
				case "syncStatuses":
					SyncService.clearCollection(getActivity());
					syncType |= SyncService.FLAG_SYNC_COLLECTION;
					break;
				case "syncPlays":
					SyncService.clearPlays(getActivity());
					syncType |= SyncService.FLAG_SYNC_PLAYS;
					break;
				case "syncBuddies":
					SyncService.clearBuddies(getActivity());
					syncType |= SyncService.FLAG_SYNC_BUDDIES;
					break;
			}
		}
	}
}