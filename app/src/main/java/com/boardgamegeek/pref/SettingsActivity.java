package com.boardgamegeek.pref;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.mikepenz.aboutlibraries.Libs;

import java.util.HashMap;
import java.util.List;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {
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
		prepareLayout();
		buildLegacyPreferences();
	}

	private void prepareLayout() {
		ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
		View content = root.getChildAt(0);
		LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_settings, null);

		root.removeAllViews();
		toolbarContainer.addView(content);
		root.addView(toolbarContainer);

		Toolbar toolBar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
		toolBar.setTitle(getTitle());
		toolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		toolBar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void buildLegacyPreferences() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			String action = getIntent().getAction();
			if (action != null) {
				Integer fragmentId = mFragmentMap.get(action);
				if (fragmentId != null) {
					addPreferencesFromResource(fragmentId);
				}
			} else {
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

			((Preference) findPreference("open_source_licenses")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					new Libs.Builder()
						.withFields(R.string.class.getFields())
						.withLibraries("OkHttp", "DragSortListView", "Hugo", "PhotoView", "RangeSeekBar", "StickyListHeaders")
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
		return fragmentName.equals(PrefFragment.class.getName());
	}
}