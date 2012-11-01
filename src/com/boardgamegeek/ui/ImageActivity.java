package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ForumsUtils;

public class ImageActivity extends SimpleSinglePaneActivity {
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";

	private String mGameName;
	private int mGameId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);
		mGameId = intent.getIntExtra(ForumsUtils.KEY_GAME_ID, BggContract.INVALID_ID);
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