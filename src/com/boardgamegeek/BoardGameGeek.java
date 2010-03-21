package com.boardgamegeek;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class BoardGameGeek extends Activity {

	private final String LOG_TAG = "BoardGameGeek";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow type-to-search
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		setContentView(R.layout.main);

		((TextView) findViewById(R.id.version)).setText(getVersionDescription());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// inflate the menu from XML
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.search:
			onSearchRequested();
			return true;
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.credits:
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog);
			dialog.setTitle(R.string.thanks_title);
			dialog.show();
			return true;
		}
		return false;
	}

	private String getVersionDescription() {
		try {
			PackageManager pm = getPackageManager();
			PackageInfo pInfo = pm.getPackageInfo(getPackageName(), 0);
			return "\nVersion " + pInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e(LOG_TAG, "NameNotFoundException in getVersion", e);
		}
		return "";
	}
}