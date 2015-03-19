package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ActivityUtils;

import hugo.weaving.DebugLog;

public class BuddyCollectionActivity extends SimpleSinglePaneActivity implements BuddyCollectionFragment.Callbacks {
	private String mBuddyName;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBuddyName = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);

		if (!TextUtils.isEmpty(mBuddyName)) {
			ActionBar bar = getSupportActionBar();
			bar.setSubtitle(mBuddyName);
		}
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new BuddyCollectionFragment();
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ActivityUtils.navigateUpToBuddy(this, mBuddyName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCollectionStatusChanged(String status) {
		String text = getString(R.string.title_collection);
		if (!TextUtils.isEmpty(status)) {
			text += " - " + status;
		}
		getSupportActionBar().setTitle(text);
	}
}
