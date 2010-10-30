package com.boardgamegeek.ui;

import com.boardgamegeek.Preferences;
import com.boardgamegeek.R;
import com.boardgamegeek.Utility;
import com.boardgamegeek.view.AboutView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class HomeActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow type-to-search
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		setContentView(R.layout.activity_home);

		((TextView) findViewById(R.id.title_text)).setText(getTitle());
		((TextView) findViewById(R.id.version)).setText(Utility.getVersionDescription(this));
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}
	
	public void onViewDatabaseClick(View v) {
		viewDatabase();
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
		case R.id.view_database:
			viewDatabase();
			return true;
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.credits:
			startActivity(new Intent(this, AboutView.class));
			return true;
		}
		return false;
	}

	private void viewDatabase() {
		Intent intent = new Intent(this, BoardgamesActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		startActivity(intent);
	}
}