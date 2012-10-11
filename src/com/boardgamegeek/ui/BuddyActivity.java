package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.boardgamegeek.R;

public class BuddyActivity extends SimpleSinglePaneActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		String mBuddyName = intent.getStringExtra(BuddiesActivity.KEY_BUDDY_NAME);

		final ActionBar actionBar = getSupportActionBar();
		if (!TextUtils.isEmpty(mBuddyName)) {
			actionBar.setSubtitle(mBuddyName);
		}
	}
	
	@Override
	protected Fragment onCreatePane() {
		return new BuddyFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}
}
