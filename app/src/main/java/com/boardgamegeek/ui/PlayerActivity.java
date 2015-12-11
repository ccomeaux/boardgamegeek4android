package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.tasks.RenamePlayerTask;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class PlayerActivity extends SimpleSinglePaneActivity {
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_PLAYER_USERNAME = "PLAYER_USERNAME";
	private int playCount;
	private String name;
	private String username;
	private EditTextDialogFragment editTextDialogFragment;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		name = intent.getStringExtra(KEY_PLAYER_NAME);
		username = intent.getStringExtra(KEY_PLAYER_USERNAME);

		setSubtitle();
	}

	@DebugLog
	private void setSubtitle() {
		String title;
		if (TextUtils.isEmpty(name)) {
			title = username;
		} else if (TextUtils.isEmpty(username)) {
			title = name;
		} else {
			title = name + " (" + username + ")";
		}
		setSubtitle(title);
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
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count,
			(isDrawerOpen() || playCount < 0) ? "" : String.valueOf(playCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.menu_edit) {
			showDialog(name);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(@NonNull PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event.getPlayId(), event.getGameId(), event.getGameName(), event.getThumbnailUrl(), event.getImageUrl());
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(@NonNull PlaysCountChangedEvent event) {
		playCount = event.getCount();
		supportInvalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(@NonNull RenamePlayerTask.Event event) {
		name = event.getPlayerName();
		getIntent().putExtra(KEY_PLAYER_NAME, name);
		setSubtitle();
		// recreate fragment to load the list with the new location
		getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
		createFragment();
		editTextDialogFragment = null;

		if (!TextUtils.isEmpty(event.getMessage())) {
			Snackbar.make(rootContainer, event.getMessage(), Snackbar.LENGTH_LONG).show();
		}
	}

	@DebugLog
	private void showDialog(final String oldName) {
		if (editTextDialogFragment == null) {
			editTextDialogFragment = EditTextDialogFragment.newInstance(R.string.title_edit_player, (ViewGroup) findViewById(R.id.root_container), new EditTextDialogListener() {
				@Override
				public void onFinishEditDialog(String inputText) {
					if (!TextUtils.isEmpty(inputText)) {
						RenamePlayerTask task = new RenamePlayerTask(PlayerActivity.this, username, oldName, inputText);
						TaskUtils.executeAsyncTask(task);
					}
				}
			});
		}
		editTextDialogFragment.setText(oldName);
		DialogUtils.showFragment(this, editTextDialogFragment, "edit_player");
	}
}