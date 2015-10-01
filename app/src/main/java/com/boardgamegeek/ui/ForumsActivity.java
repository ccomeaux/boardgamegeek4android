package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;

import com.boardgamegeek.R;

public class ForumsActivity extends TopLevelSinglePaneActivity {

	@Override
	protected Fragment onCreatePane() {
		return new ForumsFragment();
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_forums;
	}
}
