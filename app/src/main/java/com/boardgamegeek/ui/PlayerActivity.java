package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ResolverUtils;

public class PlayerActivity extends SimpleSinglePaneActivity implements PlaysFragment.Callbacks {
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_PLAYER_USERNAME = "PLAYER_USERNAME";
	private String mName;
	private String mUsername;
	private AlertDialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mName = intent.getStringExtra(KEY_PLAYER_NAME);
		mUsername = intent.getStringExtra(KEY_PLAYER_USERNAME);

		setTitle();
	}

	protected void setTitle() {
		String title;
		if (TextUtils.isEmpty(mName)) {
			title = mUsername;
		} else if (TextUtils.isEmpty(mUsername)) {
			title = mName;
		} else {
			title = mName + " (" + mUsername + ")";
		}
		getSupportActionBar().setSubtitle(title);
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
		return R.menu.edit;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_edit) {
			showDialog(mName);
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
		// TODO display in action bar
	}

	@Override
	public void onSortChanged(String sortName) {
		// sorting not allowed
	}

	public void showDialog(final String oldName) {
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
						String newName = editText.getText().toString();
						new Task().execute(oldName, newName);
					}
				}).create();
			mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
		mDialog.show();
	}

	private class Task extends AsyncTask<String, Void, String> {
		private static final String selectionWithoutUsername = "play_players." + PlayPlayers.NAME + "=? AND ("
			+ PlayPlayers.USER_NAME + "=? OR " + PlayPlayers.USER_NAME + " IS NULL)";
		private static final String selectionWithUsername = "play_players." + PlayPlayers.NAME + "=? AND "
			+ PlayPlayers.USER_NAME + "=?";

		public Task() {
		}

		@Override
		protected String doInBackground(String... params) {
			String oldName = params[0];
			String newName = params[1];

			ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
			String selection = TextUtils.isEmpty(mUsername) ? selectionWithoutUsername : selectionWithUsername;

			List<Integer> playIds = ResolverUtils.queryInts(getContentResolver(), Plays.buildPlayersUri(),
				Plays.PLAY_ID, Plays.SYNC_STATUS + "=? AND (" + selection + ")",
				TextUtils.isEmpty(mUsername) ? new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED), oldName, "" }
					: new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED), oldName, mUsername });
			if (playIds.size() > 0) {
				ContentValues values = new ContentValues();
				values.put(Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
				for (Integer playId : playIds) {
					Uri uri = Plays.buildPlayUri(playId);
					batch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
				}
			}

			batch.add(ContentProviderOperation
				.newUpdate(Plays.buildPlayersUri())
				.withValue(PlayPlayers.NAME, newName)
				.withSelection(selection,
					TextUtils.isEmpty(mUsername) ? new String[] { oldName, "" } : new String[] { oldName, mUsername })
				.build());

			ResolverUtils.applyBatch(PlayerActivity.this, batch);

			SyncService.sync(PlayerActivity.this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);

			mName = newName;
			getIntent().putExtra(KEY_PLAYER_NAME, newName);

			return getString(R.string.msg_play_player_change, oldName, newName);
		}

		@Override
		protected void onPostExecute(String result) {
			setTitle();

			// recreate fragment to load the list with the new location
			getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
			createFragment();

			if (!TextUtils.isEmpty(result)) {
				Toast.makeText(PlayerActivity.this, result, Toast.LENGTH_LONG).show();
			}
		}
	}
}