package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.sorter.PlayersSorterFactory;
import com.boardgamegeek.util.ToolbarUtils;

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
		return R.menu.players;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (isDrawerOpen()) {
			menu.findItem(R.id.menu_sort).setVisible(false);
			ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, "");
		} else {
			menu.findItem(R.id.menu_sort).setVisible(true);
			PlayersFragment fragment = (PlayersFragment) getFragment();
			if (fragment != null) {
				if (fragment.getSort() == PlayersSorterFactory.TYPE_QUANTITY) {
					menu.findItem(R.id.menu_sort_quantity).setChecked(true);
				} else {
					menu.findItem(R.id.menu_sort_name).setChecked(true);
				}
			}
			ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, mCount <= 0 ? "" : String.valueOf(mCount));
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case R.id.menu_sort_name:
				((PlayersFragment) getFragment()).setSort(PlayersSorterFactory.TYPE_NAME);
				return true;
			case R.id.menu_sort_quantity:
				((PlayersFragment) getFragment()).setSort(PlayersSorterFactory.TYPE_QUANTITY);
				return true;
		}
		return super.onOptionsItemSelected(item);
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
