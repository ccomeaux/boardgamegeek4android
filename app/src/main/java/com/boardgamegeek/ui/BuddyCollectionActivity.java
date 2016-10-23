package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.events.CollectionStatusChangedEvent;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import hugo.weaving.DebugLog;

public class BuddyCollectionActivity extends SimpleSinglePaneActivity {
	private String buddyName;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		buddyName = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);

		if (!TextUtils.isEmpty(buddyName)) {
			ActionBar bar = getSupportActionBar();
			if (bar != null) {
				bar.setSubtitle(buddyName);
			}
		}
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("BuddyCollection")
				.putContentId(buddyName));
		}
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new BuddyCollectionFragment();
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ActivityUtils.navigateUpToBuddy(this, buddyName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	@Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
	public void onEvent(CollectionStatusChangedEvent event) {
		String text = buddyName;
		if (!TextUtils.isEmpty(event.getDescription())) {
			text += " - " + event.getDescription();
		}
		ActionBar bar = getSupportActionBar();
		if (bar != null) {
			bar.setSubtitle(text);
		}
	}
}
