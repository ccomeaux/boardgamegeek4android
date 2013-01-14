package com.boardgamegeek.pref;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
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

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if ("syncStatuses".equals(key)) {
			SyncService.clearCollection(this);
		}
	}
}