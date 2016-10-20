package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GameDetailActivity extends SimpleSinglePaneActivity {
	private int gameId;
	private String gameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		String title = intent.getStringExtra(ActivityUtils.KEY_TITLE);
		gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(title);
			actionBar.setSubtitle(gameName);
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GameDetail")
				.putContentName(title));
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GameDetailFragment();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (gameId == BggContract.INVALID_ID) {
					onBackPressed();
				} else {
					ActivityUtils.navigateUpToGame(this, gameId, gameName);
				}
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
