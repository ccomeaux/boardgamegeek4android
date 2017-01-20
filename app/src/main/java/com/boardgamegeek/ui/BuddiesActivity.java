package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;

import com.boardgamegeek.R;
import com.boardgamegeek.events.BuddiesCountChangedEvent;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import hugo.weaving.DebugLog;

public class BuddiesActivity extends TopLevelSinglePaneActivity {
	private int numberOfBuddies = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Buddies"));
		}
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane() {
		return new BuddiesFragment();
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, numberOfBuddies <= 0 ? "" : String.format("%,d", numberOfBuddies));
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	protected int getOptionsMenuId() {
		return R.menu.buddies;
	}

	@DebugLog
	@Override
	protected int getDrawerResId() {
		return R.string.title_buddies;
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
	public void onEvent(BuddiesCountChangedEvent event) {
		numberOfBuddies = event.getCount();
		supportInvalidateOptionsMenu();
		invalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(BuddySelectedEvent event) {
		ActivityUtils.startBuddyActivity(this, event.getBuddyName(), event.getBuddyFullName());
	}
}
