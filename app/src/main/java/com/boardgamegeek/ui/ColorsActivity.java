package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

public class ColorsActivity extends SimpleSinglePaneActivity {
	private int mGameId;
	private String mGameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGameId = BggContract.Games.getGameId(getIntent().getData());
		mGameName = getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME);

		if (!TextUtils.isEmpty(mGameName)) {
			getSupportActionBar().setSubtitle(mGameName);
		}
	}


	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ColorsFragment();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ActivityUtils.navigateUpToGame(this, mGameId, mGameName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
