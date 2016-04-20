package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import org.greenrobot.eventbus.Subscribe;

import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class GamePlaysActivity extends SimpleSinglePaneActivity {
	private int mGameId;
	private String mGameName;
	@State int mCount = -1;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		mGameId = BggContract.Games.getGameId(getIntent().getData());
		mGameName = getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME);

		if (!TextUtils.isEmpty(mGameName)) {
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setSubtitle(mGameName);
			}
		}
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
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
	@Subscribe
	public void onEvent(PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event.getPlayId(), event.getGameId(), event.getGameName(), event.getThumbnailUrl(), event.getImageUrl());
	}

	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(PlaysCountChangedEvent event) {
		mCount = event.getCount();
		supportInvalidateOptionsMenu();
	}
}
