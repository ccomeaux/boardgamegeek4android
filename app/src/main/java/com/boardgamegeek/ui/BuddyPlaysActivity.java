package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.util.ToolbarUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import hugo.weaving.DebugLog;

public class BuddyPlaysActivity extends SimpleSinglePaneActivity {
	private static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	private String buddyName;
	private int numberOfPlays = -1;

	public static void start(Context context, String buddyName) {
		Intent starter = new Intent(context, BuddyPlaysActivity.class);
		starter.putExtra(KEY_BUDDY_NAME, buddyName);
		context.startActivity(starter);
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!TextUtils.isEmpty(buddyName)) {
			ActionBar bar = getSupportActionBar();
			if (bar != null) {
				bar.setSubtitle(buddyName);
			}
		}
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("BuddyPlays")
				.putContentId(buddyName));
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		buddyName = intent.getStringExtra(KEY_BUDDY_NAME);
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return PlaysFragment.newInstanceForBuddy(buddyName);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.text_only;
	}

	@Override
	public boolean onPrepareOptionsMenu(@NotNull Menu menu) {
		String countDescription = numberOfPlays <= 0 ? "" : String.valueOf(numberOfPlays);
		ToolbarUtils.setActionBarText(menu, R.id.menu_text, countDescription);
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				BuddyActivity.startUp(this, buddyName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(PlaysCountChangedEvent event) {
		numberOfPlays = event.getCount();
		supportInvalidateOptionsMenu();
	}
}
