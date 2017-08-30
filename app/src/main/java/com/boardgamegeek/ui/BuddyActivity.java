package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.tasks.AddUsernameToPlayerTask;
import com.boardgamegeek.tasks.BuddyNicknameUpdateTask;
import com.boardgamegeek.tasks.RenamePlayerTask;
import com.boardgamegeek.tasks.sync.SyncUserTask.CompletedEvent;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class BuddyActivity extends SimpleSinglePaneActivity {
	private static final String KEY_BUDDY_NAME = "BUDDY_NAME";
	private static final String KEY_PLAYER_NAME = "PLAYER_NAME";

	private String name;
	private String username;

	public static void start(Context context, String username, String playerName) {
		Intent starter = createIntent(context, username, playerName);
		if (starter != null) context.startActivity(starter);
	}

	public static void startUp(Context context, String username) {
		startUp(context, username, null);
	}

	public static void startUp(Context context, String username, String playerName) {
		Intent intent = createIntent(context, username, playerName);
		if (intent != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			context.startActivity(intent);
		}

	}

	@Nullable
	public static Intent createIntent(Context context, String username, String playerName) {
		if (TextUtils.isEmpty(username) && TextUtils.isEmpty(playerName)) {
			Timber.w("Unable to create a BuddyActivity intent - missing both a username and a player name");
			return null;
		}
		Intent intent = new Intent(context, BuddyActivity.class);
		intent.putExtra(KEY_BUDDY_NAME, username);
		intent.putExtra(KEY_PLAYER_NAME, playerName);
		return intent;
	}

	@Override
	@DebugLog
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (TextUtils.isEmpty(name) && TextUtils.isEmpty(username)) finish();
		setSubtitle();

		EventBus.getDefault().removeStickyEvent(BuddySelectedEvent.class);
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Buddy")
				.putContentId(username)
				.putContentName(name));
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		name = intent.getStringExtra(KEY_PLAYER_NAME);
		username = intent.getStringExtra(KEY_BUDDY_NAME);
	}

	@Override
	@DebugLog
	protected Fragment onCreatePane(Intent intent) {
		return BuddyFragment.newInstance(username, name);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.buddy;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		UIUtils.showMenuItem(menu, R.id.add_username, TextUtils.isEmpty(username));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_username) {
			EditTextDialogFragment editTextDialogFragment = EditTextDialogFragment.newUsernameInstance(R.string.title_add_username, null, new EditTextDialogListener() {
				@Override
				public void onFinishEditDialog(String inputText) {
					if (!TextUtils.isEmpty(inputText)) {
						AddUsernameToPlayerTask task = new AddUsernameToPlayerTask(BuddyActivity.this, name, inputText);
						TaskUtils.executeAsyncTask(task);
					}
				}
			});
			DialogUtils.showFragment(this, editTextDialogFragment, "add_username");
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(BuddyNicknameUpdateTask.Event event) {
		showSnackbar(event.getMessage());
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(AddUsernameToPlayerTask.Event event) {
		if (event.isSuccessful()) {
			username = event.getUsername();
			getIntent().putExtra(KEY_BUDDY_NAME, username);
			setSubtitle();

			recreateFragment();
		}
		showSnackbar(event.getMessage());
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CompletedEvent event) {
		if (!TextUtils.isEmpty(event.getErrorMessage()) && PreferencesUtils.getSyncShowErrors(this)) {
			showSnackbar(event.getErrorMessage());
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(@NonNull RenamePlayerTask.Event event) {
		name = event.getPlayerName();
		getIntent().putExtra(KEY_PLAYER_NAME, name);
		setSubtitle();

		recreateFragment();

		showSnackbar(event.getMessage());
	}

	@DebugLog
	private void setSubtitle() {
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
		if (!TextUtils.isEmpty(message) && rootContainer != null) {
			Snackbar.make(rootContainer, message, Snackbar.LENGTH_LONG).show();
		}
	}
}
