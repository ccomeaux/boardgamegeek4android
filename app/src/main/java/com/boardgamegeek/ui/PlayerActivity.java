package com.boardgamegeek.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.tasks.RenamePlayerTask;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class PlayerActivity extends SimpleSinglePaneActivity {
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_PLAYER_USERNAME = "PLAYER_USERNAME";
	private int mCount;
	private String mName;
	private String mUsername;
	private AlertDialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mName = intent.getStringExtra(KEY_PLAYER_NAME);
		mUsername = intent.getStringExtra(KEY_PLAYER_USERNAME);

		setSubtitle();
	}

	private void setSubtitle() {
		String title;
		if (TextUtils.isEmpty(mName)) {
			title = mUsername;
		} else if (TextUtils.isEmpty(mUsername)) {
			title = mName;
		} else {
			title = mName + " (" + mUsername + ")";
		}
		setSubtitle(title);
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		final Intent intent = getIntent();
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_PLAYER);
		arguments.putString(PlaysFragment.KEY_PLAYER_NAME, intent.getStringExtra(KEY_PLAYER_NAME));
		arguments.putString(PlaysFragment.KEY_USER_NAME, intent.getStringExtra(KEY_PLAYER_USERNAME));
		return arguments;
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.player;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count,
			(isDrawerOpen() || mCount < 0) ? "" : String.valueOf(mCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_edit) {
			showDialog(mName);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	public void onEvent(PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event.playId, event.gameId, event.gameName, event.thumbnailUrl, event.imageUrl);
	}

	@DebugLog
	public void onEvent(PlaysCountChangedEvent event) {
		mCount = event.count;
		supportInvalidateOptionsMenu();
	}

	public void onEvent(RenamePlayerTask.Event event) {
		mName = event.playerName;
		getIntent().putExtra(KEY_PLAYER_NAME, mName);
		setSubtitle();
		// recreate fragment to load the list with the new location
		getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
		createFragment();
	}

	private void showDialog(final String oldName) {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_edit_text, (ViewGroup) findViewById(R.id.root_container), false);

		final EditText editText = (EditText) view.findViewById(R.id.edit_text);
		if (!TextUtils.isEmpty(oldName)) {
			editText.setText(oldName);
			editText.setSelection(0, oldName.length());
		}

		if (mDialog == null) {
			mDialog = new AlertDialog.Builder(this).setView(view).setTitle(R.string.title_edit_player)
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String newName = editText.getText().toString().trim();
						RenamePlayerTask task = new RenamePlayerTask(PlayerActivity.this, mUsername, oldName, newName);
						TaskUtils.executeAsyncTask(task);
					}
				}).create();
			mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
		mDialog.show();
	}
}