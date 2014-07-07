package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.BuddyUtils;

public class PlayerActivity extends SimpleSinglePaneActivity implements PlaysFragment.Callbacks {
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_PLAYER_USERNAME = "PLAYER_USERNAME";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		String name = intent.getStringExtra(KEY_PLAYER_NAME);
		String username = intent.getStringExtra(KEY_PLAYER_USERNAME);

		String title;
		if (TextUtils.isEmpty(name)) {
			title = username;
		} else if (TextUtils.isEmpty(username)) {
			title = name;
		} else {
			title = name + " (" + username + ")";
		}
		getSupportActionBar().setSubtitle(title);
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		final Intent intent = getIntent();
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_PLAYER);
		arguments.putString(PlaysFragment.KEY_PLAYER_NAME, intent.getStringExtra(KEY_PLAYER_NAME));
		arguments.putString(BuddyUtils.KEY_BUDDY_NAME, intent.getStringExtra(KEY_PLAYER_USERNAME));
		return arguments;
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl) {
		ActivityUtils.launchPlay(this, playId, gameId, gameName, thumbnailUrl);
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
