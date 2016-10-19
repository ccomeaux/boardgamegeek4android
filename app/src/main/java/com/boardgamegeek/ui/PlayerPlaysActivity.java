package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Menu;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;

import hugo.weaving.DebugLog;

public class PlayerPlaysActivity extends SimpleSinglePaneActivity {
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_PLAYER_USERNAME = "PLAYER_USERNAME";
	private int playCount;
	private String name;
	private String username;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		name = intent.getStringExtra(KEY_PLAYER_NAME);
		username = intent.getStringExtra(KEY_PLAYER_USERNAME);

		String name = calculateName();
		setSubtitle(name);
		if (savedInstanceState == null) {
			final ContentViewEvent event = new ContentViewEvent()
				.putContentType("PlayerPlays")
				.putContentName(name);
			if (!TextUtils.isEmpty(username)) {
				event.putContentId(username);
			}
			Answers.getInstance().logContentView(event);
		}
	}

	@DebugLog
	private String calculateName() {
		if (TextUtils.isEmpty(name)) {
			return username;
		} else if (TextUtils.isEmpty(username)) {
			return name;
		}
		return String.format("%s (%s)", name, username);
	}

	@NonNull
	@DebugLog
	@Override
	protected Bundle onBeforeArgumentsSet(@NonNull Bundle arguments) {
		final Intent intent = getIntent();
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_PLAYER);
		arguments.putString(PlaysFragment.KEY_PLAYER_NAME, intent.getStringExtra(KEY_PLAYER_NAME));
		arguments.putString(PlaysFragment.KEY_USER_NAME, intent.getStringExtra(KEY_PLAYER_USERNAME));
		return arguments;
	}

	@NonNull
	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@DebugLog
	@Override
	protected int getOptionsMenuId() {
		return R.menu.player;
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, playCount < 0 ? "" : String.valueOf(playCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(@NonNull PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event.getPlayId(), event.getGameId(), event.getGameName(), event.getThumbnailUrl(), event.getImageUrl());
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(@NonNull PlaysCountChangedEvent event) {
		playCount = event.getCount();
		supportInvalidateOptionsMenu();
	}
}