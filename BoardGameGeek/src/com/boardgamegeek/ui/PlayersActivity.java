package com.boardgamegeek.ui;

import com.actionbarsherlock.view.Menu;
import com.boardgamegeek.R;
import com.boardgamegeek.util.ActivityUtils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class PlayersActivity extends TopLevelSinglePaneActivity implements PlayersFragment.Callbacks {
	private static final String KEY_COUNT = "KEY_COUNT";
	private int mCount = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mCount = savedInstanceState.getInt(KEY_COUNT);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_COUNT, mCount);
	}

	@Override
	protected Fragment onCreatePane() {
		return new PlayersFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.text_only;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ActivityUtils.setActionBarText(menu, R.id.menu_list_count,
			(isDrawerOpen() || mCount <= 0) ? "" : String.valueOf(mCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_players;
	}

	@Override
	public boolean onPlayerSelected(String name, String username) {
		Intent intent = new Intent(this, PlayerActivity.class);
		intent.putExtra(PlayerActivity.KEY_PLAYER_NAME, name);
		intent.putExtra(PlayerActivity.KEY_PLAYER_USERNAME, username);
		startActivity(intent);
		return true;
	}

	@Override
	public void onPlayerCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
	}
}
