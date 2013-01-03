package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.database.PlayPersister;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(LogPlayActivity.class);

	private static final int HELP_VERSION = 1;
	private static final int REQUEST_ADD_PLAYER = 0;
	private static final int NOTIFICATION_ID = 2;

	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_START_TIME = "START_TIME";
	private static final String KEY_LENGTH_SHOWN = "LENGTH_SHOWN";
	private static final String KEY_LOCATION_SHOWN = "LOCATION_SHOWN";
	private static final String KEY_INCOMPLETE_SHOWN = "INCOMPLETE_SHOWN";
	private static final String KEY_NO_WIN_STATS_SHOWN = "NO_WIN_STATS_SHOWN";
	private static final String KEY_COMMENTS_SHOWN = "COMMENTS_SHOWN";
	private static final String KEY_PLAYERS_SHOWN = "PLAYERS_SHOWN";
	private static final String KEY_DELETE_ON_BACK = "DELETE_ON_BACK";

	private LocationAdapter mLocationAdapter;

	private Play mPlay;
	private Play mOriginalPlay;
	private int mNextPlayerTag = 1;
	private boolean mLaunchingActivity;
	private long mStartTime;

	private Button mDateButton;
	private EditText mQuantityView;
	private EditText mLengthView;
	private AutoCompleteTextView mLocationView;
	private CheckBox mIncompleteView;
	private CheckBox mNoWinStatsView;
	private EditText mCommentsView;
	private TextView mPlayerLabel;
	private LinearLayout mPlayerList;

	private boolean mLengthShown;
	private boolean mLocationShown;
	private boolean mIncompleteShown;
	private boolean mNoWinStatsShown;
	private boolean mCommentsShown;
	private boolean mPlayersShown;
	private boolean mDeleteOnBack;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplay);
		getSupportActionBar().setHomeButtonEnabled(false);
		setUiVariables();

		final Intent intent = getIntent();
		int playId = intent.getIntExtra(KEY_PLAY_ID, BggContract.INVALID_ID);
		int gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		String gameName = intent.getStringExtra(KEY_GAME_NAME);
		mStartTime = intent.getLongExtra(KEY_START_TIME, 0);

		if (gameId <= 0) {
			LOGW(TAG, "Can't log a play without a game ID.");
			Toast.makeText(this, "Can't log a play without a game ID.", Toast.LENGTH_LONG).show();
			finish();
		}
		changeName(gameName);

		if (savedInstanceState != null) {
			mPlay = new Play(savedInstanceState, "P");
			mOriginalPlay = new Play(savedInstanceState, "O");
			mLengthShown = savedInstanceState.getBoolean(KEY_LENGTH_SHOWN);
			mLocationShown = savedInstanceState.getBoolean(KEY_LOCATION_SHOWN);
			mIncompleteShown = savedInstanceState.getBoolean(KEY_INCOMPLETE_SHOWN);
			mNoWinStatsShown = savedInstanceState.getBoolean(KEY_NO_WIN_STATS_SHOWN);
			mCommentsShown = savedInstanceState.getBoolean(KEY_COMMENTS_SHOWN);
			mPlayersShown = savedInstanceState.getBoolean(KEY_PLAYERS_SHOWN);
			mDeleteOnBack = savedInstanceState.getBoolean(KEY_DELETE_ON_BACK);
		} else {
			mPlay = new Play(playId, gameId, gameName);
			if (playId > 0) {
				mDeleteOnBack = false;
				getSupportLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);
				getSupportLoaderManager().restartLoader(PlayerQuery._TOKEN, null, this);
			} else {
				mDeleteOnBack = true;
				save(Play.SYNC_STATUS_IN_PROGRESS);
			}
			mOriginalPlay = new Play(mPlay);
		}

		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// TODO: refactor to quick log without this activity (probably need to use AccountManager)
			logPlay();
			finish();
		} else if (!Intent.ACTION_EDIT.equals(intent.getAction())) {
			LOGW(TAG, "Received bad intent action: " + intent.getAction());
			finish();
		}

		bindUi();

		UIUtils.showHelpDialog(this, BggApplication.HELP_LOGPLAY_KEY, HELP_VERSION, R.string.help_logplay);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mLocationAdapter = new LocationAdapter(this);
		mLocationView.setAdapter(mLocationAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mLaunchingActivity = false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mPlay.saveState(outState, "P");
		mOriginalPlay.saveState(outState, "O");
		outState.putBoolean(KEY_LENGTH_SHOWN, mLengthShown);
		outState.putBoolean(KEY_LOCATION_SHOWN, mLocationShown);
		outState.putBoolean(KEY_INCOMPLETE_SHOWN, mIncompleteShown);
		outState.putBoolean(KEY_NO_WIN_STATS_SHOWN, mNoWinStatsShown);
		outState.putBoolean(KEY_COMMENTS_SHOWN, mCommentsShown);
		outState.putBoolean(KEY_PLAYERS_SHOWN, mPlayersShown);
		outState.putBoolean(KEY_DELETE_ON_BACK, mDeleteOnBack);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (!isFinishing() && !mLaunchingActivity) {
			save(Play.SYNC_STATUS_IN_PROGRESS);
			Toast.makeText(this, "Saving draft of play.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onBackPressed() {
		cancel();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.logplay, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_start).setVisible(!mPlay.hasEnded());
		menu.findItem(R.id.menu_add_field).setEnabled(
			hideLength() || hideLocation() || hideNoWinStats() || hideIncomplete() || hideComments() || hidePlayers());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_send:
				logPlay();
				finish();
				return true;
			case R.id.menu_save:
				save(Play.SYNC_STATUS_IN_PROGRESS);
				finish();
				return true;
			case R.id.menu_start:
				startPlay();
				return true;
			case R.id.menu_cancel:
				cancel();
				return true;
			case R.id.menu_add_field:
				final CharSequence[] array = createAddFieldArray();
				if (array == null || array.length == 0) {
					return false;
				}
				promptAddField(array);
				return true;
		}
		return false;
	}

	private void promptAddField(final CharSequence[] array) {
		new AlertDialog.Builder(this).setTitle(R.string.add_field)
			.setItems(array, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Resources r = getResources();
					String selection = array[which].toString();
					if (selection == r.getString(R.string.length)) {
						mLengthShown = true;
						findViewById(R.id.log_play_length_label).setVisibility(View.VISIBLE);
						mLengthView.setVisibility(View.VISIBLE);
					} else if (selection == r.getString(R.string.location)) {
						mLocationShown = true;
						findViewById(R.id.log_play_location_label).setVisibility(View.VISIBLE);
						mLocationView.setVisibility(View.VISIBLE);
					} else if (selection == r.getString(R.string.incomplete)) {
						mIncompleteShown = true;
						findViewById(R.id.log_play_incomplete).setVisibility(View.VISIBLE);
					} else if (selection == r.getString(R.string.noWinStats)) {
						mNoWinStatsShown = true;
						findViewById(R.id.log_play_no_win_stats).setVisibility(View.VISIBLE);
					} else if (selection == r.getString(R.string.comments)) {
						mCommentsShown = true;
						findViewById(R.id.log_play_comments_label).setVisibility(View.VISIBLE);
						findViewById(R.id.log_play_comments).setVisibility(View.VISIBLE);
					} else if (selection == r.getString(R.string.players)) {
						mPlayersShown = true;
						mPlayerLabel.setVisibility(View.VISIBLE);
						findViewById(R.id.log_play_players_add).setVisibility(View.VISIBLE);
					}
				}
			}).show();
	}

	private void logPlay() {
		save(Play.SYNC_STATUS_PENDING_UPDATE);
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
		triggerUpload();
	}

	private void triggerUpload() {
		SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	private void save(int syncStatus) {
		if (syncStatus != Play.SYNC_STATUS_PENDING_DELETE) {
			captureForm();
		}
		mPlay.SyncStatus = syncStatus;
		PlayPersister.save(getContentResolver(), mPlay);
	}

	private void startPlay() {
		save(Play.SYNC_STATUS_IN_PROGRESS);

		Intent intent = new Intent(this, LogPlayActivity.class);
		intent.setAction(Intent.ACTION_EDIT);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(KEY_PLAY_ID, mPlay.PlayId);
		intent.putExtra(KEY_GAME_ID, mPlay.GameId);
		intent.putExtra(KEY_GAME_NAME, mPlay.GameName);
		intent.putExtra(KEY_START_TIME, System.currentTimeMillis());

		launchStartNotification(intent);
	}

	private void launchStartNotification(Intent intent) {
		String title = getString(R.string.notification_title);
		String message = String.format(getString(R.string.notification_text_playing), mPlay.GameName);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle(title).setContentText(message).setTicker(message).setSmallIcon(R.drawable.ic_stat_play)
			.setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, builder.build());
	}

	private void cancel() {
		captureForm();
		if (mPlay.equals(mOriginalPlay)) {
			if (mDeleteOnBack) {
				save(Play.SYNC_STATUS_PENDING_DELETE);
			}
			triggerUpload();
			setResult(RESULT_CANCELED);
			finish();
		} else {
			if (mDeleteOnBack) {
				ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_message,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							save(Play.SYNC_STATUS_PENDING_DELETE);
							triggerUpload();
							setResult(RESULT_CANCELED);
							finish();
						}
					}).show();
			} else {
				ActivityUtils.createCancelDialog(this).show();
			}
		}
	}

	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<CharSequence>();

		if (hideLength()) {
			list.add(r.getString(R.string.length));
		}
		if (hideLocation()) {
			list.add(r.getString(R.string.location));
		}
		if (hideIncomplete()) {
			list.add(r.getString(R.string.incomplete));
		}
		if (hideNoWinStats()) {
			list.add(r.getString(R.string.noWinStats));
		}
		if (hideComments()) {
			list.add(r.getString(R.string.comments));
		}
		if (hidePlayers()) {
			list.add(r.getString(R.string.players));
		}

		CharSequence[] array = {};
		array = list.toArray(array);
		return array;
	}

	public void onDateClick(View v) {
		DialogFragment fragment = new DatePickerFragment(mDateSetListener);
		Bundle bundle = new Bundle();
		bundle.putInt(DatePickerFragment.KEY_YEAR, mPlay.Year);
		bundle.putInt(DatePickerFragment.KEY_MONTH, mPlay.Month);
		bundle.putInt(DatePickerFragment.KEY_DAY, mPlay.Day);
		fragment.setArguments(bundle);
		fragment.show(getSupportFragmentManager(), "datePicker");
	}

	public void onAddPlayerClick(View v) {
		if (BggApplication.getInstance().getPlayLoggingEditPlayer()) {
			addPlayer(new Intent(), REQUEST_ADD_PLAYER);
		} else {
			addPlayer(new Player());
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Player player = new Player(data);
			if (requestCode == REQUEST_ADD_PLAYER) {
				addPlayer(player);
			} else {
				((PlayerRow) mPlayerList.findViewWithTag(requestCode)).setPlayer(player);
			}
		}
	}

	private void bindUi() {
		bindUiPlay();
		bindUiPlayers();
	}

	private void bindUiPlay() {
		setDateButtonText();
		mQuantityView.setText(String.valueOf(mPlay.Quantity));
		mLengthView.setText(String.valueOf(mPlay.Length));
		mLocationView.setText(mPlay.Location);
		mIncompleteView.setChecked(mPlay.Incomplete);
		mNoWinStatsView.setChecked(mPlay.NoWinStats);
		mCommentsView.setText(mPlay.Comments);
	}

	private void bindUiPlayers() {
		mPlayerList.removeAllViews();
		for (Player player : mPlay.getPlayers()) {
			addPlayer(player);
		}
		hideFields();
	}

	private void addPlayer(Player p) {
		PlayerRow row = new PlayerRow(this);
		row.setPlayer(p);
		row.setTag(mNextPlayerTag++);
		row.setOnEditListener(onPlayerEdit());
		row.setOnDeleteListener(onPlayerDelete());
		mPlayerList.addView(row);
		calculatePlayerCount();
	}

	private void calculatePlayerCount() {
		Resources r = getResources();
		int playerCount = mPlayerList.getChildCount();
		if (playerCount <= 0) {
			mPlayerLabel.setText(r.getString(R.string.title_players));
		} else {
			mPlayerLabel.setText(r.getString(R.string.title_players) + " - " + String.valueOf(playerCount));
		}
	}

	private OnClickListener onPlayerEdit() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayerRow row = (PlayerRow) v;
				Player player = row.getPlayer();
				addPlayer(player.toIntent(), (Integer) row.getTag());
			}
		};
	}

	private OnClickListener onPlayerDelete() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(LogPlayActivity.this, R.string.msg_player_deleted, Toast.LENGTH_SHORT).show();
				mPlayerList.removeView(v);
				calculatePlayerCount();
			}
		};
	}

	private void addPlayer(Intent intent, int requestCode) {
		mLaunchingActivity = true;
		intent.setClass(LogPlayActivity.this, LogPlayerActivity.class);
		intent.putExtra(LogPlayerActivity.KEY_GAME_ID, mPlay.GameId);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, mPlay.GameName);
		startActivityForResult(intent, requestCode);
	}

	private void setUiVariables() {
		mDateButton = (Button) findViewById(R.id.log_play_date);
		mQuantityView = (EditText) findViewById(R.id.log_play_quantity);
		mLengthView = (EditText) findViewById(R.id.log_play_length);
		mLocationView = (AutoCompleteTextView) findViewById(R.id.log_play_location);
		mIncompleteView = (CheckBox) findViewById(R.id.log_play_incomplete);
		mNoWinStatsView = (CheckBox) findViewById(R.id.log_play_no_win_stats);
		mCommentsView = (EditText) findViewById(R.id.log_play_comments);
		mPlayerLabel = (TextView) findViewById(R.id.log_play_players_label);
		mPlayerList = (LinearLayout) findViewById(R.id.log_play_player_list);
	}

	private void hideFields() {
		findViewById(R.id.log_play_length_label).setVisibility(hideLength() ? View.GONE : View.VISIBLE);
		mLengthView.setVisibility(hideLength() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_location_label).setVisibility(hideLocation() ? View.GONE : View.VISIBLE);
		mLocationView.setVisibility(hideLocation() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_incomplete).setVisibility(hideIncomplete() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_no_win_stats).setVisibility(hideNoWinStats() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_comments_label).setVisibility(hideComments() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_comments).setVisibility(hideComments() ? View.GONE : View.VISIBLE);
		mPlayerLabel.setVisibility(hidePlayers() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_players_add).setVisibility(hidePlayers() ? View.GONE : View.VISIBLE);
	}

	private boolean hideLength() {
		return BggApplication.getInstance().getPlayLoggingHideLength() && !mLengthShown && !(mPlay.Length > 0);
	}

	private boolean hideLocation() {
		return BggApplication.getInstance().getPlayLoggingHideLocation() && !mLocationShown
			&& TextUtils.isEmpty(mPlay.Location);
	}

	private boolean hideIncomplete() {
		return BggApplication.getInstance().getPlayLoggingHideIncomplete() && !mIncompleteShown && !mPlay.Incomplete;
	}

	private boolean hideNoWinStats() {
		return BggApplication.getInstance().getPlayLoggingHideNoWinStats() && !mNoWinStatsShown && !mPlay.NoWinStats;
	}

	private boolean hideComments() {
		return BggApplication.getInstance().getPlayLoggingHideComments() && !mCommentsShown
			&& TextUtils.isEmpty(mPlay.Comments);
	}

	private boolean hidePlayers() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerList() && !mPlayersShown
			&& (mPlay.getPlayers().size() == 0);
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			if (mPlay != null) {
				mPlay.setDate(year, monthOfYear, dayOfMonth);
				setDateButtonText();
			}
		}
	};

	private void setDateButtonText() {
		mDateButton.setText(mPlay.getDateForDisplay());
	}

	/**
	 * Captures the data in the form in the mPlay object
	 */
	private void captureForm() {
		// date info already captured
		mPlay.Quantity = StringUtils.parseInt(mQuantityView.getText().toString().trim(), 1);
		mPlay.Length = StringUtils.parseInt(mLengthView.getText().toString().trim());
		mPlay.Location = mLocationView.getText().toString().trim();
		mPlay.Incomplete = mIncompleteView.isChecked();
		mPlay.NoWinStats = mNoWinStatsView.isChecked();
		mPlay.Comments = mCommentsView.getText().toString().trim();
		mPlay.clearPlayers();
		for (int i = 0; i < mPlayerList.getChildCount(); i++) {
			View view = mPlayerList.getChildAt(i);
			if (view instanceof PlayerRow) {
				PlayerRow pr = (PlayerRow) view;
				Player p = pr.getPlayer();
				mPlay.addPlayer(p);
			}
		}
	}

	private class LocationAdapter extends CursorAdapter {
		public LocationAdapter(Context context) {
			super(context, null, false);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getLayoutInflater().inflate(R.layout.autocomplete_item, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final TextView textView = (TextView) view.findViewById(R.id.autocomplete_item);
			textView.setText(cursor.getString(LocationQuery.LOCATION));
		}

		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(LocationQuery.LOCATION);
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			String selection = null;
			String[] selectionArgs = null;
			if (!TextUtils.isEmpty(constraint)) {
				selection = Plays.LOCATION + " LIKE ?";
				selectionArgs = new String[] { constraint + "%" };
			}
			return getContentResolver().query(Plays.buildLocationsUri(), LocationQuery.PROJECTION, selection,
				selectionArgs, null);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case PlayQuery._TOKEN:
				loader = new CursorLoader(this, mPlay.uri(), PlayQuery.PROJECTION, null, null, null);
				break;
			case PlayerQuery._TOKEN:
				loader = new CursorLoader(this, mPlay.playerUri(), PlayerQuery.PROJECTION, null, null, null);
				break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		switch (loader.getId()) {
			case PlayQuery._TOKEN:
				if (cursor == null || !cursor.moveToFirst()) {
					return;
				}

				mPlay.fromCursor(cursor);
				mOriginalPlay = new Play(mPlay);
				if (mStartTime > 0) {
					mPlay.Length = DateTimeUtils.howManyMinutesOld(mStartTime);
				}
				changeName(mPlay.GameName);
				bindUiPlay();
				break;
			case PlayerQuery._TOKEN:
				mPlay.clearPlayers();
				while (cursor.moveToNext()) {
					Player player = new Player(cursor);
					mPlay.addPlayer(player);
				}
				mOriginalPlay = new Play(mPlay);
				bindUiPlayers();
				break;
			default:
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private void changeName(String gameName) {
		if (!TextUtils.isEmpty(gameName)) {
			getSupportActionBar().setSubtitle(gameName);
		}
	}

	private interface PlayQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays.PLAY_ID, PlayItems.NAME, PlayItems.OBJECT_ID, Plays.DATE, Plays.LOCATION,
			Plays.LENGTH, Plays.QUANTITY, Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.COMMENTS, };
	}

	private interface PlayerQuery {
		int _TOKEN = 0x02;
		String[] PROJECTION = { PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.START_POSITION, PlayPlayers.COLOR,
			PlayPlayers.SCORE, PlayPlayers.RATING, PlayPlayers.NEW, PlayPlayers.WIN, };
	}

	private interface LocationQuery {
		String[] PROJECTION = { Plays._ID, Plays.LOCATION };
		int LOCATION = 1;
	}

	@SuppressLint("ValidFragment")
	public static class DatePickerFragment extends DialogFragment {
		public static final String KEY_YEAR = "YEAR";
		public static final String KEY_MONTH = "MONTH";
		public static final String KEY_DAY = "DAY";

		private OnDateSetListener mListener;

		public DatePickerFragment() {
		}

		public DatePickerFragment(DatePickerDialog.OnDateSetListener listener) {
			mListener = listener;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle b = getArguments();
			int year = b.getInt(KEY_YEAR);
			int month = b.getInt(KEY_MONTH);
			int day = b.getInt(KEY_DAY);
			return new DatePickerDialog(getActivity(), mListener, year, month, day);
		}
	}
}
