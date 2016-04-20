package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.events.PlaysFilterChangedEvent;
import com.boardgamegeek.events.PlaysSortChangedEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.ToolbarUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import hugo.weaving.DebugLog;

public class PlaysActivity extends SimpleSinglePaneActivity {
	private static final int HELP_VERSION = 1;
	private int playCount;
	private String sortName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		HelpUtils.showHelpDialog(this, HelpUtils.HELP_PLAYS_KEY, HELP_VERSION, R.string.help_plays);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.plays;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean hide = playCount <= 0;
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count,
			hide ? "" : String.valueOf(playCount),
			hide ? "" : sortName);
		return super.onPrepareOptionsMenu(menu);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event.getPlayId(), event.getGameId(), event.getGameName(), event.getThumbnailUrl(), event.getImageUrl());
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(PlaysCountChangedEvent event) {
		playCount = event.getCount();
		supportInvalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(PlaysFilterChangedEvent event) {
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (event.getType() == Play.SYNC_STATUS_ALL) {
				actionBar.setSubtitle("");
			} else {
				actionBar.setSubtitle(event.getDescription());
			}
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(PlaysSortChangedEvent event) {
		sortName = event.getDescription();
		supportInvalidateOptionsMenu();
	}
}
