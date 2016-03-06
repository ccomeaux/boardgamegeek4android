package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.tasks.BuddyNicknameUpdateTask;
import com.boardgamegeek.tasks.RenamePlayerTask;
import com.boardgamegeek.util.ActivityUtils;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class BuddyActivity extends SimpleSinglePaneActivity {
	private String username;

	@Override
	@DebugLog
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		final String name = getIntent().getStringExtra(ActivityUtils.KEY_PLAYER_NAME);
		username = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);
		setSubtitle(username, name);

		EventBus.getDefault().removeStickyEvent(BuddySelectedEvent.class);
	}

	@Override
	@DebugLog
	protected Fragment onCreatePane(Intent intent) {
		return new BuddyFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.buddy;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.add_username);
		if (item != null) {
			item.setVisible(TextUtils.isEmpty(username));
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_username) {
			showSnackbar("Adding username...");
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(BuddyNicknameUpdateTask.Event event) {
		showSnackbar(event.getMessage());
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(@NonNull RenamePlayerTask.Event event) {
		final String username = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);
		String name = event.getPlayerName();
		getIntent().putExtra(ActivityUtils.KEY_PLAYER_NAME, name);
		setSubtitle(username, name);

		recreateFragment();

		showSnackbar(event.getMessage());
	}

	@DebugLog
	private void setSubtitle(String username, String name) {
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			String subtitle;
			if (TextUtils.isEmpty(username)) {
				subtitle = name;
			} else {
				subtitle = username;
			}
			actionBar.setSubtitle(subtitle);
		}
	}

	private void showSnackbar(String message) {
		if (!TextUtils.isEmpty(message)) {
			Snackbar.make(rootContainer, message, Snackbar.LENGTH_LONG).show();
		}
	}
}
