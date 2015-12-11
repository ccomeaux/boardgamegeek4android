package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

import hugo.weaving.DebugLog;

public class ColorsActivity extends SimpleSinglePaneActivity {
	private int gameId;
	private String gameName;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameId = BggContract.Games.getGameId(getIntent().getData());
		gameName = getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME);

		if (!TextUtils.isEmpty(gameName)) {
			ActionBar supportActionBar = getSupportActionBar();
			if (supportActionBar != null) {
				supportActionBar.setSubtitle(gameName);
			}
		}
	}

	@NonNull
	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ColorsFragment();
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ActivityUtils.navigateUpToGame(this, gameId, gameName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
