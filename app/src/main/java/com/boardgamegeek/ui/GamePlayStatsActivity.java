package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GamePlayStatsActivity extends SimpleSinglePaneActivity {
	private int gameId;
	private String gameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameId = BggContract.Games.getGameId(getIntent().getData());
		gameName = getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME);

		if (!TextUtils.isEmpty(gameName)) {
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setSubtitle(gameName);
			}
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GamePlayStats")
				.putContentId(String.valueOf(gameId))
				.putContentName(gameName));
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GamePlayStatsFragment();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ActivityUtils.navigateUpToGame(this, gameId, gameName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}