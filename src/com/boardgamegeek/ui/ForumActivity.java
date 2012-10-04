package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ForumsUtils;

public class ForumActivity extends SimpleSinglePaneActivity {
	private int mGameId;
	private String mGameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		String forumTitle = intent.getStringExtra(ForumsUtils.KEY_FORUM_TITLE);
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);
		mGameId = intent.getIntExtra(ForumsUtils.KEY_GAME_ID, BggContract.INVALID_ID);

		if (!TextUtils.isEmpty(forumTitle)) {
			final ActionBar actionBar = getSupportActionBar();
			if (TextUtils.isEmpty(mGameName)) {
				actionBar.setSubtitle(forumTitle);
			} else {
				actionBar.setTitle(forumTitle);
				actionBar.setSubtitle(mGameName);
			}
		}
	}

	@Override
	protected Fragment onCreatePane() {
		return new ForumFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mGameId == BggContract.INVALID_ID) {
					startActivity(new Intent(this, ForumsActivity.class));
				} else {
					ActivityUtils.launchGame(this, mGameId, mGameName);
				}
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
