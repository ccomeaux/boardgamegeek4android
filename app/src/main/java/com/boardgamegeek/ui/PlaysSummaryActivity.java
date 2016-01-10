package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;

import com.boardgamegeek.R;

public class PlaysSummaryActivity extends TopLevelSinglePaneActivity {
	@Override
	protected Fragment onCreatePane() {
		return new PlaysSummaryFragment();
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_plays;
	}
}
