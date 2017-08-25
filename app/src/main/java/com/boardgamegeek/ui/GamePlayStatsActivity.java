package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GamePlayStatsActivity extends SimpleSinglePaneActivity {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_HEADER_COLOR = "HEADER_COLOR";
	private int gameId;
	private String gameName;
	@ColorInt private int headerColor;

	public static void start(Context context, int gameId, String gameName, @ColorInt int headerColor) {
		Intent starter = new Intent(context, GamePlayStatsActivity.class);
		starter.putExtra(KEY_GAME_ID, gameId);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_HEADER_COLOR, headerColor);
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameId = getIntent().getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = getIntent().getStringExtra(KEY_GAME_NAME);
		headerColor = getIntent().getIntExtra(KEY_HEADER_COLOR, getResources().getColor(R.color.accent));

		if (!TextUtils.isEmpty(gameName)) {
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setSubtitle(gameName);
			}
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GamePlayStats")
				.putContentId(String.valueOf(gameId))
				.putContentName(gameName));
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return GamePlayStatsFragment.newInstance(gameId, headerColor);
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