package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class CommentsActivity extends SimpleSinglePaneActivity {
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_SORT_TYPE = "SORT_TYPE";
	private static final int SORT_TYPE_USER = 0;
	private static final int SORT_TYPE_RATING = 1;

	private int gameId;
	private String gameName;
	private int sortType;

	public static void startComments(Context context, Uri gameUri, String gameName) {
		Intent starter = new Intent(context, CommentsActivity.class);
		starter.setData(gameUri);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_SORT_TYPE, SORT_TYPE_USER);
		context.startActivity(starter);
	}

	public static void startRating(Context context, Uri gameUri, String gameName) {
		Intent starter = new Intent(context, CommentsActivity.class);
		starter.setData(gameUri);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_SORT_TYPE, SORT_TYPE_RATING);
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (sortType == SORT_TYPE_RATING) {
				actionBar.setTitle(R.string.title_ratings);
			}
			if (!TextUtils.isEmpty(gameName)) {
				actionBar.setSubtitle(gameName);
			}
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GameComments")
				.putContentId(String.valueOf(gameId))
				.putContentName(gameName));
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		gameId = BggContract.Games.getGameId(intent.getData());
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		sortType = intent.getIntExtra(KEY_SORT_TYPE, SORT_TYPE_USER);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return CommentsFragment.newInstance(gameId, sortType == SORT_TYPE_RATING);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				GameActivity.startUp(this, gameId, gameName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
