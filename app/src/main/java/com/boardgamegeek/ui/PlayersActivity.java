package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.sorter.PlayersSorterFactory;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import icepick.Icepick;
import icepick.State;

public class PlayersActivity extends SimpleSinglePaneActivity implements PlayersFragment.Callbacks {
	@State int playerCount = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlayersFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.players;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_sort).setVisible(true);
		PlayersFragment fragment = (PlayersFragment) getFragment();
		if (fragment != null) {
			if (fragment.getSort() == PlayersSorterFactory.TYPE_QUANTITY) {
				menu.findItem(R.id.menu_sort_quantity).setChecked(true);
			} else {
				menu.findItem(R.id.menu_sort_name).setChecked(true);
			}
		}
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, playerCount <= 0 ? "" : String.valueOf(playerCount));
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
		ActivityUtils.startBuddyActivity(this, username, name);
		return true;
	}

	@Override
	public void onPlayerCountChanged(int count) {
		playerCount = count;
		supportInvalidateOptionsMenu();
	}
}
