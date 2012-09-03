package com.boardgamegeek.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;

public class BuddiesActivity extends SimpleSinglePaneActivity implements BuddiesFragment.Callbacks {
	@Override
	protected Fragment onCreatePane() {
		return new BuddiesFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}

	@Override
	public boolean onBuddySelected(int buddyId) {
		startActivity(new Intent(Intent.ACTION_VIEW, Buddies.buildBuddyUri(buddyId)));
		return false;
	}
}
