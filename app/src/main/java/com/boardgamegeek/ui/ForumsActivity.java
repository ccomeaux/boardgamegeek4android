package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class ForumsActivity extends TopLevelSinglePaneActivity {

	public static void startUp(Context context) {
		Intent starter = new Intent(context, ForumsActivity.class);
		starter.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Forums"));
		}
	}

	@Override
	protected Fragment onCreatePane() {
		return new ForumsFragment();
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_forums;
	}
}
