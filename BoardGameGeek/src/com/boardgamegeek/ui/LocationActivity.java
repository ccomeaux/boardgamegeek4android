package com.boardgamegeek.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;

public class LocationActivity extends SimpleSinglePaneActivity implements PlaysFragment.Callbacks {
	public static final String KEY_LOCATION_NAME = "LOCATION_NAME";
	private int mCount;
	private String mLocationName;
	private AlertDialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mLocationName = intent.getStringExtra(KEY_LOCATION_NAME);
		setTitle(mLocationName);
	}

	private void setTitle(String title) {
		if (TextUtils.isEmpty(title)) {
			title = getString(R.string.no_location);
		}
		getSupportActionBar().setSubtitle(title);
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		final Intent intent = getIntent();
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_LOCATION);
		arguments.putString(PlaysFragment.KEY_LOCATION, intent.getStringExtra(KEY_LOCATION_NAME));
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
		ActivityUtils.setActionBarText(menu, R.id.menu_list_count,
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

	@Override
	public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		ActivityUtils.startPlayActivity(this, playId, gameId, gameName, thumbnailUrl, imageUrl);
		return false;
	}

	@Override
	public void onPlayCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onSortChanged(String sortName) {
		// sorting not allowed
	}

	public void showDialog(final String oldLocation) {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_edit_location, (ViewGroup) findViewById(R.id.root_container),
			false);

		final EditText editText = (EditText) view.findViewById(R.id.edit_location);
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
						String newLocation = editText.getText().toString();
						new Task().execute(newLocation);
					}
				}).create();
			mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
		mDialog.show();
	}

	private class Task extends AsyncTask<String, Void, String> {
		public Task() {
		}

		@Override
		protected String doInBackground(String... params) {
			String newLocation = params[0];

			ContentValues values = new ContentValues(2);
			values.put(Plays.LOCATION, newLocation);
			values.put(Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
			int count = getContentResolver().update(Plays.CONTENT_URI, values, Plays.LOCATION + "=?",
				new String[] { mLocationName });

			String result = null;
			if (count > 0) {
				result = getString(R.string.msg_play_location_change, count, mLocationName, newLocation);
				SyncService.sync(LocationActivity.this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
			} else {
				result = getString(R.string.msg_play_location_change);
			}

			mLocationName = newLocation;
			getIntent().putExtra(KEY_LOCATION_NAME, newLocation);

			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			setTitle(mLocationName);

			// recreate fragment to load the list with the new location
			getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
			createFragment();

			if (!TextUtils.isEmpty(result)) {
				Toast.makeText(LocationActivity.this, result, Toast.LENGTH_LONG).show();
			}
		}
	}
}
