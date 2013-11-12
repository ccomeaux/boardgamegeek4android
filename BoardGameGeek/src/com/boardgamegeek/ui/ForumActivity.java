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
	private String mForumId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		String forumTitle = intent.getStringExtra(ForumsUtils.KEY_FORUM_TITLE);
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);
		mGameId = intent.getIntExtra(ForumsUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mForumId = intent.getStringExtra(ForumsUtils.KEY_FORUM_ID);

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
	protected Fragment onCreatePane(Intent intent) {
		return new ForumFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mGameId == BggContract.INVALID_ID) {
					Intent intent = new Intent(this, ForumsActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				} else {
					ActivityUtils.navigateUpToGame(this, mGameId, mGameName);
				}
				finish();
				return true;
			case R.id.view:
				ActivityUtils.linkToBgg(this, "forum/" + mForumId);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
