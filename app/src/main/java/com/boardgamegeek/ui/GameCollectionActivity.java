package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

public class GameCollectionActivity extends SimpleSinglePaneActivity {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_COLLECTION_ID = "COLLECTION_ID";
	public static final String KEY_COLLECTION_NAME = "COLLECTION_NAME";
	private int mGameId;
	private String mGameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGameId = getIntent().getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = getIntent().getStringExtra(KEY_GAME_NAME);
		String collectionName = getIntent().getStringExtra(KEY_COLLECTION_NAME);

		if (!TextUtils.isEmpty(collectionName)) {
			ActionBar bar = getSupportActionBar();
			bar.setSubtitle(collectionName);
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GameCollectionFragment();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mGameId == BggContract.INVALID_ID) {
					onBackPressed();
				} else {
					ActivityUtils.navigateUpToGame(this, mGameId, mGameName);
				}
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
