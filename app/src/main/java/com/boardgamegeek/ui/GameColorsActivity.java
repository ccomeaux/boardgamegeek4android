package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.provider.BggContract;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import hugo.weaving.DebugLog;

public class GameColorsActivity extends SimpleSinglePaneActivity {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_ICON_COLOR = "ICON_COLOR";
	private int gameId;
	private String gameName;
	@ColorInt private int iconColor;

	public static void start(Context context, int gameId, String gameName, @ColorInt int iconColor) {
		Intent starter = new Intent(context, GameColorsActivity.class);
		starter.putExtra(KEY_GAME_ID, gameId);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_ICON_COLOR, iconColor);
		context.startActivity(starter);
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameId = getIntent().getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = getIntent().getStringExtra(KEY_GAME_NAME);
		iconColor = getIntent().getIntExtra(KEY_ICON_COLOR, Color.TRANSPARENT);

		if (!TextUtils.isEmpty(gameName)) {
			ActionBar supportActionBar = getSupportActionBar();
			if (supportActionBar != null) {
				supportActionBar.setSubtitle(gameName);
			}
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GameColors")
				.putContentId(String.valueOf(gameId))
				.putContentName(gameName));
		}
	}

	@NonNull
	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return ColorsFragment.newInstance(gameId, iconColor);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				GameActivity.startUp(this, gameId, gameName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
