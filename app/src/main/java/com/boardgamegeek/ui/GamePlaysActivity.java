package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class GamePlaysActivity extends SimpleSinglePaneActivity implements PlaysFragment.Callbacks {
	private static final String KEY_COUNT = "COUNT";
	private int mGameId;
	private String mGameName;
	private int mCount = -1;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mCount = savedInstanceState.getInt(KEY_COUNT);
		}

		mGameId = BggContract.Games.getGameId(getIntent().getData());
		mGameName = getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME);

		if (!TextUtils.isEmpty(mGameName)) {
			ActionBar bar = getSupportActionBar();
			bar.setSubtitle(mGameName);
		}
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_COUNT, mCount);
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.text_only;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		String countDescription = mCount <= 0 ? "" : String.valueOf(mCount);
		ToolbarUtils.setActionBarText(menu, R.id.menu_text, countDescription);
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
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

	@DebugLog
	@Override
	public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		ActivityUtils.startPlayActivity(this, playId, gameId, gameName, thumbnailUrl, imageUrl);
		return false;
	}

	@DebugLog
	@Override
	public void onPlayCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
	}

	@DebugLog
	@Override
	public void onSortChanged(String sortName) {
		// sort not supported in this activity
	}
}
