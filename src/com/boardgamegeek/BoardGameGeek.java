package com.boardgamegeek;

import com.boardgamegeek.view.AboutView;
import com.boardgamegeek.view.BoardGameListView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class BoardGameGeek extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow type-to-search
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		setContentView(R.layout.main);
		((TextView) findViewById(R.id.version)).setText(Utility.getVersionDescription(this));

		((Button) findViewById(R.id.searchButton)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onSearchRequested();
			}
		});

		((Button) findViewById(R.id.viewButton)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				viewDatabase();
			}
		});
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
		Intent intent = new Intent(this, BoardGameListView.class);
		intent.setAction(Intent.ACTION_VIEW);
		startActivity(intent);
	}
}