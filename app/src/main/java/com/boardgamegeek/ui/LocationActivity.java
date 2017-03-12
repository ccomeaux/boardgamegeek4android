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
import com.boardgamegeek.events.LocationSelectedEvent;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.tasks.RenameLocationTask;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.boardgamegeek.util.fabric.DataManipulationEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import hugo.weaving.DebugLog;

public class LocationActivity extends SimpleSinglePaneActivity {
	private int playCount;
	private String locationName;
	private EditTextDialogFragment editTextDialogFragment;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		locationName = intent.getStringExtra(ActivityUtils.KEY_LOCATION_NAME);
		setSubtitle();

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Location")
				.putContentName(locationName));
		}

		EventBus.getDefault().removeStickyEvent(LocationSelectedEvent.class);
	}

	@DebugLog
	private void setSubtitle() {
		String text = locationName;
		if (TextUtils.isEmpty(locationName)) {
			text = getString(R.string.no_location);
		}
		setSubtitle(text);
	}

	@NonNull
	@DebugLog
	@Override
	protected Bundle onBeforeArgumentsSet(@NonNull Bundle arguments) {
		final Intent intent = getIntent();
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_LOCATION);
		arguments.putString(ActivityUtils.KEY_LOCATION, intent.getStringExtra(ActivityUtils.KEY_LOCATION_NAME));
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
		return R.menu.location;
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, playCount < 0 ? "" : String.valueOf(playCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.menu_edit) {
			showDialog(locationName);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(@NonNull PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(@NonNull PlaysCountChangedEvent event) {
		playCount = event.getCount();
		supportInvalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(@NonNull RenameLocationTask.Event event) {
		locationName = event.getLocationName();
		getIntent().putExtra(ActivityUtils.KEY_LOCATION_NAME, locationName);
		setSubtitle();
		// recreate fragment to load the list with the new location
		getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
		createFragment();
		editTextDialogFragment = null;

		if (!TextUtils.isEmpty(event.getMessage()) && rootContainer != null) {
			Snackbar.make(rootContainer, event.getMessage(), Snackbar.LENGTH_LONG).show();
		}
	}

	@DebugLog
	private void showDialog(final String oldLocation) {
		if (editTextDialogFragment == null) {
			editTextDialogFragment = EditTextDialogFragment.newInstance(R.string.title_edit_location, (ViewGroup) findViewById(R.id.root_container), new EditTextDialogListener() {
				@Override
				public void onFinishEditDialog(String inputText) {
					if (!TextUtils.isEmpty(inputText)) {
						DataManipulationEvent.log("Location", "Edit");
						RenameLocationTask task = new RenameLocationTask(LocationActivity.this, oldLocation, inputText);
						TaskUtils.executeAsyncTask(task);
					}
				}
			});
		}
		editTextDialogFragment.setText(oldLocation);
		DialogUtils.showFragment(this, editTextDialogFragment, "edit_location");
	}
}
