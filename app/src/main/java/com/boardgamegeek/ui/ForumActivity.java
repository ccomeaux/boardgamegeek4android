package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class ForumActivity extends SimpleSinglePaneActivity {
	private static final String KEY_FORUM_ID = "FORUM_ID";
	private static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";

	private int gameId;
	private String gameName;
	private int forumId;
	private String forumTitle;

	public static void start(Context context, int forumId, String forumTitle, int gameId, String gameName) {
		Intent starter = createIntent(context, forumId, forumTitle, gameId, gameName);
		context.startActivity(starter);
	}

	public static void startUp(Context context, int forumId, String forumTitle, int gameId, String gameName) {
		Intent starter = createIntent(context, forumId, forumTitle, gameId, gameName);
		starter.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@NonNull
	private static Intent createIntent(Context context, int forumId, String forumTitle, int gameId, String gameName) {
		Intent starter = new Intent(context, ForumActivity.class);
		starter.putExtra(KEY_FORUM_ID, forumId);
		starter.putExtra(KEY_FORUM_TITLE, forumTitle);
		starter.putExtra(KEY_GAME_ID, gameId);
		starter.putExtra(KEY_GAME_NAME, gameName);
		return starter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = intent.getStringExtra(KEY_FORUM_TITLE);

		if (!TextUtils.isEmpty(forumTitle)) {
			final ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				if (TextUtils.isEmpty(gameName)) {
					actionBar.setSubtitle(forumTitle);
				} else {
					actionBar.setTitle(forumTitle);
					actionBar.setSubtitle(gameName);
				}
			}
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Forum")
				.putContentId(String.valueOf(forumId))
				.putContentName(forumTitle));
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return ForumFragment.newInstance(forumId, forumTitle, gameId, gameName);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (gameId == BggContract.INVALID_ID) {
					ForumsActivity.startUp(this);
				} else {
					GameForumsActivity.startUp(this, Games.buildGameUri(gameId), gameName);
				}
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "forum/" + forumId);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
