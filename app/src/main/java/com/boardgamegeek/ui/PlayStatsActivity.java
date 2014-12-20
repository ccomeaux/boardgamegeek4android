package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;

import com.boardgamegeek.R;

public class PlayStatsActivity extends TopLevelSinglePaneActivity {

	@Override
	protected Fragment onCreatePane() {
		return new PlayStatsFragment();
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_play_stats;
	}
}
