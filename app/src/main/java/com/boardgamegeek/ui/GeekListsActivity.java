package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GeekListsActivity extends TopLevelSinglePaneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("GeekLists"));
		}
	}

	@Override
	protected Fragment onCreatePane() {
		return new GeekListsFragment();
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_geeklists;
	}
}
