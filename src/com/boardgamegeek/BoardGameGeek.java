package com.boardgamegeek;

import android.app.Activity;
import android.content.Intent;
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
		((TextView) findViewById(R.id.version)).setText(Utility.getVersionDescription(this));
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
		case R.id.view_cache:
			Intent intent = new Intent(this, ViewBoardGameList.class);
			intent.setAction(Intent.ACTION_VIEW);
			startActivity(intent);
			return true;
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.credits:
			Utility.CreateAboutDialog(this).show();
			return true;
		}
		return false;
	}
}