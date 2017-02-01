package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;

import hugo.weaving.DebugLog;

public class PlaysSummaryActivity extends TopLevelSinglePaneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("PlaysSummary"));
		}
	}

	@Override
	protected Fragment onCreatePane() {
		return new PlaysSummaryFragment();
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_plays;
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event);
	}

}
