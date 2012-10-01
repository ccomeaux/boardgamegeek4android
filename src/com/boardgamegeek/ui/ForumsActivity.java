package com.boardgamegeek.ui;

import com.boardgamegeek.R;

import android.support.v4.app.Fragment;

public class ForumsActivity extends SimpleSinglePaneActivity {

	@Override
	protected Fragment onCreatePane() {
		return new ForumsFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}
}
