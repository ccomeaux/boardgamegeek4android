package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ForumsUtils;

public class ForumActivity extends SimpleSinglePaneActivity {
	private int mGameId;
	private String mGameName;
	private int mForumId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		String forumTitle = intent.getStringExtra(ForumsUtils.KEY_FORUM_TITLE);
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);
		mGameId = intent.getIntExtra(ForumsUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mForumId = intent.getIntExtra(ForumsUtils.KEY_FORUM_ID, BggContract.INVALID_ID);

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
				Intent intent = null;
				if (mGameId == BggContract.INVALID_ID) {
					intent = new Intent(this, ForumsActivity.class);
				} else {
					intent = new Intent(this, GameForumsActivity.class);
					intent.setData(Games.buildGameUri(mGameId));
					intent.putExtra(ForumsUtils.KEY_GAME_NAME, mGameName);
				}
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);;
				startActivity(intent);
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "forum/" + mForumId);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
