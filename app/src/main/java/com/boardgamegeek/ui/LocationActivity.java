package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.tasks.RenameLocationTask;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment;
import com.boardgamegeek.ui.dialog.EditTextDialogFragment.EditTextDialogListener;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.boardgamegeek.util.fabric.DataManipulationEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import hugo.weaving.DebugLog;

public class LocationActivity extends SimpleSinglePaneActivity implements EditTextDialogListener {
	private static final String KEY_LOCATION_NAME = "LOCATION_NAME";

	private int playCount;
	private String locationName;

	public static void start(Context context, String locationName) {
		Intent starter = new Intent(context, LocationActivity.class);
		starter.putExtra(KEY_LOCATION_NAME, locationName);
		context.startActivity(starter);
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setSubtitle();

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Location")
				.putContentName(locationName));
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		locationName = intent.getStringExtra(KEY_LOCATION_NAME);
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
	protected Fragment onCreatePane(Intent intent) {
		return PlaysFragment.newInstanceForLocation(locationName);
	}

	@DebugLog
	@Override
	protected int getOptionsMenuId() {
		return R.menu.location;
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(@NotNull Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, playCount < 0 ? "" : String.valueOf(playCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		if (item.getItemId() == R.id.menu_edit) {
			showDialog(locationName);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
		getIntent().putExtra(KEY_LOCATION_NAME, locationName);
		setSubtitle();
		// recreate fragment to load the list with the new location
		getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
		createFragment();

		if (!TextUtils.isEmpty(event.getMessage()) && rootContainer != null) {
			Snackbar.make(rootContainer, event.getMessage(), Snackbar.LENGTH_LONG).show();
		}
	}

	@DebugLog
	private void showDialog(final String oldLocation) {
		EditTextDialogFragment editTextDialogFragment = EditTextDialogFragment.newInstance(R.string.title_edit_location, oldLocation);
		DialogUtils.showFragment(this, editTextDialogFragment, "edit_location");
	}

	@Override
	public void onFinishEditDialog(@NotNull String text, @Nullable String originalText) {
		if (!TextUtils.isEmpty(text)) {
			DataManipulationEvent.log("Location", "Edit");
			RenameLocationTask task = new RenameLocationTask(LocationActivity.this, originalText, text);
			TaskUtils.executeAsyncTask(task);
		}
	}
}
