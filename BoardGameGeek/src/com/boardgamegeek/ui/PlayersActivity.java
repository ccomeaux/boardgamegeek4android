package com.boardgamegeek.ui;

import com.boardgamegeek.R;

import android.content.Intent;
import android.support.v4.app.Fragment;

public class PlayersActivity extends TopLevelSinglePaneActivity implements PlayersFragment.Callbacks {
	@Override
	protected Fragment onCreatePane() {
		return new PlayersFragment();
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
}
