package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

public class CommentsActivity extends SimpleSinglePaneActivity {
	public static final int SORT_USER = 0;
	public static final int SORT_RATING = 1;

	private int mGameId;
	private String mGameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGameId = BggContract.Games.getGameId(getIntent().getData());
		mGameName = getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME);
		int sort = getIntent().getIntExtra(ActivityUtils.KEY_SORT, SORT_USER);

		ActionBar bar = getSupportActionBar();
		if (sort == SORT_RATING) {
			bar.setTitle(R.string.title_ratings);
		}
		if (!TextUtils.isEmpty(mGameName)) {
			bar.setSubtitle(mGameName);
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new CommentsFragment();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ActivityUtils.navigateUpToGame(this, mGameId, mGameName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
