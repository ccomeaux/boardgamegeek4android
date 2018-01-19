package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.boardgamegeek.provider.BggContract;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GameDetailActivity extends SimpleSinglePaneActivity {
	private static final String KEY_TITLE = "TITLE";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_QUERY_TOKEN = "QUERY_TOKEN";

	private String title;
	private int gameId;
	private String gameName;
	private int queryToken;

	public static void start(Context context, String title, int gameId, String gameName, int queryToken) {
		Intent starter = new Intent(context, GameDetailActivity.class);
		starter.putExtra(KEY_TITLE, title);
		starter.putExtra(KEY_GAME_ID, gameId);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_QUERY_TOKEN, queryToken);
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(gameName);
			actionBar.setSubtitle(title);
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GameDetail")
				.putContentName(title));
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		title = intent.getStringExtra(KEY_TITLE);
		gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		queryToken = intent.getIntExtra(KEY_QUERY_TOKEN, 0);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return GameDetailFragment.newInstance(gameId, queryToken);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (gameId == BggContract.INVALID_ID) {
					onBackPressed();
				} else {
					GameActivity.startUp(this, gameId, gameName);
				}
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
