package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class TopGamesActivity extends TopLevelSinglePaneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Top Games"));
		}
	}

	@Override
	protected Fragment onCreatePane() {
		return new TopGamesFragment();
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_top_games;
	}
}
