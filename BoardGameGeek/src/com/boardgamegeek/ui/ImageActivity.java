package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;

import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

public class ImageActivity extends SimpleSinglePaneActivity {
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_TITLE = "TITLE";

	private String mGameName;
	private int mGameId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mGameName = intent.getStringExtra(KEY_GAME_NAME);
		mGameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		String title = intent.getStringExtra(KEY_TITLE);

		getSupportActionBar().setSubtitle(TextUtils.isEmpty(title) ? mGameName : title);
	}

	@Override
	protected Fragment onCreatePane() {
		return new ImageFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mGameId == BggContract.INVALID_ID) {
					NavUtils.navigateUpFromSameTask(this);
				} else {
					ActivityUtils.navigateUpToGame(this, mGameId, mGameName);
				}
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}