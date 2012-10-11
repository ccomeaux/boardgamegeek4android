package com.boardgamegeek.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;

public class BuddiesActivity extends SimpleSinglePaneActivity implements BuddiesFragment.Callbacks {
	
	public static final String KEY_BUDDY_ID = "BUDDY_ID";
	public static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	
	@Override
	protected Fragment onCreatePane() {
		return new BuddiesFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}

	@Override
	public boolean onBuddySelected(int buddyId, String buddyName) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Buddies.buildBuddyUri(buddyId));
		intent.putExtra(KEY_BUDDY_ID, buddyId);
		intent.putExtra(KEY_BUDDY_NAME, buddyName);
		startActivity(intent);
		return false;
	}
}
