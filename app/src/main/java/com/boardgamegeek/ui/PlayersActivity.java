package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlayerSelectedEvent;
import com.boardgamegeek.events.PlayersCountChangedEvent;
import com.boardgamegeek.sorter.PlayersSorterFactory;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;

import icepick.Icepick;
import icepick.State;

public class PlayersActivity extends SimpleSinglePaneActivity {
	@State int playerCount = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Players"));
		}
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
			switch (fragment.getSort()) {
				case PlayersSorterFactory.TYPE_QUANTITY:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_quantity);
					break;
				case PlayersSorterFactory.TYPE_WINS:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_wins);
					break;
				case PlayersSorterFactory.TYPE_NAME:
				default:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_name);
					break;
			}
		}
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, playerCount <= 0 ? "" : String.format("%,d", playerCount));
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
			case R.id.menu_sort_wins:
				((PlayersFragment) getFragment()).setSort(PlayersSorterFactory.TYPE_WINS);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_players;
	}

	@Subscribe
	public void onEvent(PlayerSelectedEvent event) {
		ActivityUtils.startBuddyActivity(this, event.getUsername(), event.getName());
	}

	@Subscribe(sticky = true)
	public void onEvent(PlayersCountChangedEvent event) {
		playerCount = event.getCount();
		supportInvalidateOptionsMenu();
	}
}
