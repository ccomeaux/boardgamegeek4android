package com.boardgamegeek.ui;

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
import com.boardgamegeek.events.LocationSelectedEvent;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.tasks.RenameLocationTask;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.ToolbarUtils;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class LocationActivity extends SimpleSinglePaneActivity {
	private int mCount;
	private String mLocationName;
	private AlertDialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mLocationName = intent.getStringExtra(ActivityUtils.KEY_LOCATION_NAME);
		setSubtitle();

		EventBus.getDefault().removeStickyEvent(LocationSelectedEvent.class);
	}

	private void setSubtitle() {
		String text = mLocationName;
		if (TextUtils.isEmpty(mLocationName)) {
			text = getString(R.string.no_location);
		}
		setSubtitle(text);
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		final Intent intent = getIntent();
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_LOCATION);
		arguments.putString(PlaysFragment.KEY_LOCATION, intent.getStringExtra(ActivityUtils.KEY_LOCATION_NAME));
		return arguments;
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.location;
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
			showDialog(mLocationName);
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

	public void onEvent(RenameLocationTask.Event event) {
		mLocationName = event.locationName;
		getIntent().putExtra(ActivityUtils.KEY_LOCATION_NAME, mLocationName);
		setTitle(mLocationName);
		// recreate fragment to load the list with the new location
		getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
		createFragment();
	}

	private void showDialog(final String oldLocation) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_edit_text, (ViewGroup) findViewById(R.id.root_container), false);

		final EditText editText = (EditText) view.findViewById(R.id.edit_text);
		if (!TextUtils.isEmpty(oldLocation)) {
			editText.setText(oldLocation);
			editText.setSelection(0, oldLocation.length());
		}

		if (mDialog == null) {
			mDialog = new AlertDialog.Builder(this).setView(view).setTitle(R.string.title_edit_location)
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String newLocation = editText.getText().toString().trim();
						RenameLocationTask task = new RenameLocationTask(LocationActivity.this, oldLocation, newLocation);
						TaskUtils.executeAsyncTask(task);
					}
				}).create();
			mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
		mDialog.show();
	}
}
