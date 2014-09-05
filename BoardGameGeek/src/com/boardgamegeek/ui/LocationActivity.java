package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.util.ActivityUtils;

public class LocationActivity extends SimpleSinglePaneActivity implements PlaysFragment.Callbacks {
	public static final String KEY_LOCATION_NAME = "LOCATION_NAME";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		String name = intent.getStringExtra(KEY_LOCATION_NAME);

		getSupportActionBar().setSubtitle(name);
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		final Intent intent = getIntent();
		// TODO
		// arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_PLAYER);
		// arguments.putString(PlaysFragment.KEY_PLAYER_NAME, intent.getStringExtra(KEY_PLAYER_NAME));
		// arguments.putString(BuddyUtils.KEY_BUDDY_NAME, intent.getStringExtra(KEY_PLAYER_USERNAME));
		return arguments;
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		ActivityUtils.startPlayActivity(this, playId, gameId, gameName, thumbnailUrl, imageUrl);
		return false;
	}

	@Override
	public void onPlayCountChanged(int count) {
		// TODO display in action bar
	}

	@Override
	public void onSortChanged(String sortName) {
		// sorting not allowed
	}
}
