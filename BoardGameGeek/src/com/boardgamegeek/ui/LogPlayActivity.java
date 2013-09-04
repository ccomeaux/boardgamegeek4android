package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
import com.boardgamegeek.util.AutoCompleteAdapter;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(LogPlayActivity.class);

	private static final int HELP_VERSION = 1;
	private static final int REQUEST_ADD_PLAYER = 0;

	public static final int RESULT_UPDATED = RESULT_FIRST_USER;
	public static final String ACTION_PLAY_AGAIN = "com.boardgamegeek.intent.action.PLAY_AGAIN";
	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_END_PLAY = "END_PLAY";
	private static final String KEY_QUANTITY_SHOWN = "QUANTITY_SHOWN";
	private static final String KEY_LENGTH_SHOWN = "LENGTH_SHOWN";
	private static final String KEY_LOCATION_SHOWN = "LOCATION_SHOWN";
	private static final String KEY_INCOMPLETE_SHOWN = "INCOMPLETE_SHOWN";
	private static final String KEY_NO_WIN_STATS_SHOWN = "NO_WIN_STATS_SHOWN";
	private static final String KEY_COMMENTS_SHOWN = "COMMENTS_SHOWN";
	private static final String KEY_PLAYERS_SHOWN = "PLAYERS_SHOWN";
	private static final String KEY_DELETE_ON_CANCEL = "DELETE_ON_CANCEL";

	private Play mPlay;
	private Play mOriginalPlay;
	private boolean mLaunchingActivity;
	private Random mRandom = new Random();

	private Button mDateButton;
	private EditText mQuantityView;
	private EditText mLengthView;
	private AutoCompleteTextView mLocationView;
	private CheckBox mIncompleteView;
	private CheckBox mNoWinStatsView;
	private Chronometer mTimer;
	private EditText mCommentsView;
	private TextView mPlayerLabel;
	private LinearLayout mPlayerList;

	private boolean mPlayLoaded;
	private boolean mPlayersLoaded;
	private boolean mQuantityShown;
	private boolean mLengthShown;
	private boolean mLocationShown;
	private boolean mIncompleteShown;
	private boolean mNoWinStatsShown;
	private boolean mCommentsShown;
	private boolean mPlayersShown;
	private boolean mDeleteOnCancel;
	private boolean mEndPlay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplay);
		getSupportActionBar().setHomeButtonEnabled(false);
		setUiVariables();

		final Intent intent = getIntent();
		if (!Intent.ACTION_EDIT.equals(intent.getAction()) && !ACTION_PLAY_AGAIN.equals(intent.getAction())) {
			LOGW(TAG, "Received bad intent action: " + intent.getAction());
			finish();
		}

		int playId = intent.getIntExtra(KEY_PLAY_ID, BggContract.INVALID_ID);
		int gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		String gameName = intent.getStringExtra(KEY_GAME_NAME);
		mEndPlay = intent.getBooleanExtra(KEY_END_PLAY, false);

		if (gameId <= 0) {
			LOGW(TAG, "Can't log a play without a game ID.");
			Toast.makeText(this, "Can't log a play without a game ID.", Toast.LENGTH_LONG).show();
			finish();
		}
		changeName(gameName);

		if (savedInstanceState != null) {
			mPlay = new Play(savedInstanceState, "P");
			mOriginalPlay = new Play(savedInstanceState, "O");
			mQuantityShown = savedInstanceState.getBoolean(KEY_QUANTITY_SHOWN);
			mLengthShown = savedInstanceState.getBoolean(KEY_LENGTH_SHOWN);
			mLocationShown = savedInstanceState.getBoolean(KEY_LOCATION_SHOWN);
			mIncompleteShown = savedInstanceState.getBoolean(KEY_INCOMPLETE_SHOWN);
			mNoWinStatsShown = savedInstanceState.getBoolean(KEY_NO_WIN_STATS_SHOWN);
			mCommentsShown = savedInstanceState.getBoolean(KEY_COMMENTS_SHOWN);
			mPlayersShown = savedInstanceState.getBoolean(KEY_PLAYERS_SHOWN);
			mDeleteOnCancel = savedInstanceState.getBoolean(KEY_DELETE_ON_CANCEL);
			signalDataLoaded();
		} else {
			mPlay = new Play(playId, gameId, gameName);
			if (playId > 0) {
				// Editing an existing play
				mDeleteOnCancel = false;
				getSupportLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);
				getSupportLoaderManager().restartLoader(PlayerQuery._TOKEN, null, this);
			} else {
				// Starting a new play
				mDeleteOnCancel = true;
				saveDraft(false);
				mOriginalPlay = new Play(mPlay);
				signalDataLoaded();
			}
		}

		bindUi();

		UIUtils.showHelpDialog(this, HelpUtils.HELP_LOGPLAY_KEY, HELP_VERSION, R.string.help_logplay);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mLocationView.setAdapter(new AutoCompleteAdapter(this, Plays.LOCATION, Plays.buildLocationsUri()));
	}

	@Override
	protected void onResume() {
		super.onResume();
		mLaunchingActivity = false;
		NotificationUtils.cancel(this, NotificationUtils.ID_PLAY_TIMER);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mPlay.hasStarted()) {
			NotificationUtils.launchStartNotification(this, mPlay);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mPlay.saveState(outState, "P");
		mOriginalPlay.saveState(outState, "O");
		outState.putBoolean(KEY_QUANTITY_SHOWN, mQuantityShown);
		outState.putBoolean(KEY_LENGTH_SHOWN, mLengthShown);
		outState.putBoolean(KEY_LOCATION_SHOWN, mLocationShown);
		outState.putBoolean(KEY_INCOMPLETE_SHOWN, mIncompleteShown);
		outState.putBoolean(KEY_NO_WIN_STATS_SHOWN, mNoWinStatsShown);
		outState.putBoolean(KEY_COMMENTS_SHOWN, mCommentsShown);
		outState.putBoolean(KEY_PLAYERS_SHOWN, mPlayersShown);
		outState.putBoolean(KEY_DELETE_ON_CANCEL, mDeleteOnCancel);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (!isFinishing() && !mLaunchingActivity) {
			saveDraft(false);
		}
	}

	@Override
	public void onBackPressed() {
		saveDraft(true);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.logplay, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mPlayLoaded && mPlayersLoaded) {
			menu.findItem(R.id.menu_send).setVisible(!mPlay.hasStarted());
			menu.findItem(R.id.menu_start).setVisible(!mPlay.hasStarted() && !mPlay.hasEnded());
			menu.findItem(R.id.menu_add_field).setVisible(
				shouldHideQuantity() || shouldHideLength() || shouldHideLocation() || shouldHideNoWinStats()
					|| shouldHideIncomplete() || shouldHideComments() || shouldHidePlayers());
			menu.findItem(R.id.menu_player_order).setVisible(mPlay.getPlayers().size() > 0);
			menu.findItem(R.id.menu_save).setVisible(true);
			menu.findItem(R.id.menu_cancel).setVisible(true);
		} else {
			menu.findItem(R.id.menu_send).setVisible(false);
			menu.findItem(R.id.menu_start).setVisible(false);
			menu.findItem(R.id.menu_add_field).setVisible(false);
			menu.findItem(R.id.menu_player_order).setVisible(false);
			menu.findItem(R.id.menu_save).setVisible(false);
			menu.findItem(R.id.menu_cancel).setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_send:
				logPlay();
				return true;
			case R.id.menu_save:
				saveDraft(true);
				finish();
				return true;
			case R.id.menu_start:
				mPlay.start();
				saveDraft(false);
				bindUiPlay();
				return true;
			case R.id.menu_cancel:
				cancel();
				return true;
			case R.id.menu_add_field:
				final CharSequence[] array = createAddFieldArray();
				if (array == null || array.length == 0) {
					return false;
				}
				promptAddField(array, item);
				return true;
			case R.id.menu_pick_start_player:
				promptPickStartPlayer();
				return true;
			case R.id.menu_random_start_player:
				int newSeat = mRandom.nextInt(mPlay.getPlayers().size());
				mPlay.pickStartPlayer(newSeat);
				notifyStartPlayer();
				bindUiPlayers();
				return true;
			case R.id.menu_random_player_order:
				mPlay.randomizePlayerOrder();
				notifyStartPlayer();
				bindUiPlayers();
				return true;
		}
		return false;
	}

	private void notifyStartPlayer() {
		Player p = mPlay.getPlayerAtSeat(1);
		if (p != null) {
			String name = p.getDescsription();
			if (TextUtils.isEmpty(name)) {
				name = String.format(getResources().getString(R.string.generic_player), 1);
			}
			Toast.makeText(this, String.format(getResources().getString(R.string.notification_start_player), name),
				Toast.LENGTH_SHORT).show();
		}
	}

	private void promptPickStartPlayer() {
		CharSequence[] array = createArrayOfPlayerDescriptions();
		new AlertDialog.Builder(this).setTitle(R.string.title_pick_start_player)
			.setItems(array, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mPlay.pickStartPlayer(which);
					notifyStartPlayer();
					bindUiPlayers();
				}
			}).show();
	}

	public CharSequence[] createArrayOfPlayerDescriptions() {
		String playerPrefix = getResources().getString(R.string.generic_player);
		List<CharSequence> list = new ArrayList<CharSequence>();
		for (int i = 0; i < mPlay.getPlayers().size(); i++) {
			Player p = mPlay.getPlayers().get(i);
			String name = p.getDescsription();
			if (TextUtils.isEmpty(name)) {
				name = String.format(playerPrefix, (i + 1));
			}
			list.add(name);
		}
		CharSequence[] array = {};
		array = list.toArray(array);
		return array;
	}

	private void promptAddField(final CharSequence[] array, final MenuItem menuItem) {
		new AlertDialog.Builder(this).setTitle(R.string.add_field)
			.setItems(array, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					View viewToFocus = null;
					Resources r = getResources();
					String selection = array[which].toString();
					if (selection == r.getString(R.string.quantity)) {
						mQuantityShown = true;
						viewToFocus = mQuantityView;
					} else if (selection == r.getString(R.string.length)) {
						mLengthShown = true;
						viewToFocus = mLengthView;
					} else if (selection == r.getString(R.string.location)) {
						mLocationShown = true;
						viewToFocus = mLocationView;
					} else if (selection == r.getString(R.string.incomplete)) {
						mIncompleteShown = true;
						mIncompleteView.setChecked(true);
					} else if (selection == r.getString(R.string.noWinStats)) {
						mNoWinStatsShown = true;
						mNoWinStatsView.setChecked(true);
					} else if (selection == r.getString(R.string.comments)) {
						mCommentsShown = true;
						viewToFocus = mCommentsView;
					} else if (selection == r.getString(R.string.players)) {
						mPlayersShown = true;
					}
					supportInvalidateOptionsMenu();
					hideFields();
					if (viewToFocus != null) {
						viewToFocus.requestFocus();
					}
				}
			}).show();
	}

	private void logPlay() {
		save(Play.SYNC_STATUS_PENDING_UPDATE);
		NotificationUtils.cancel(this, NotificationUtils.ID_PLAY_TIMER);
		triggerUpload();
		Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show();
		setResult(RESULT_UPDATED);
		finish();
	}

	private void triggerUpload() {
		SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	private void saveDraft(boolean showToast) {
		save(Play.SYNC_STATUS_IN_PROGRESS);
		if (showToast) {
			Toast.makeText(this, R.string.msg_saving_draft, Toast.LENGTH_SHORT).show();
		}
	}

	private void save(int syncStatus) {
		if (syncStatus != Play.SYNC_STATUS_PENDING_DELETE) {
			captureForm();
		}
		mPlay.SyncStatus = syncStatus;
		PlayPersister.save(getContentResolver(), mPlay);
	}

	private void cancel() {
		captureForm();
		if (mPlay.equals(mOriginalPlay)) {
			if (mDeleteOnCancel) {
				save(Play.SYNC_STATUS_PENDING_DELETE);
			}
			triggerUpload();
			setResult(RESULT_UPDATED);
			finish();
		} else {
			if (mDeleteOnCancel) {
				ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							save(Play.SYNC_STATUS_PENDING_DELETE);
							triggerUpload();
							setResult(RESULT_UPDATED);
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
		if (shouldHideQuantity()) {
			list.add(r.getString(R.string.quantity));
		}
		if (shouldHideLength()) {
			list.add(r.getString(R.string.length));
		}
		if (shouldHideLocation()) {
			list.add(r.getString(R.string.location));
		}
		if (shouldHideIncomplete()) {
			list.add(r.getString(R.string.incomplete));
		}
		if (shouldHideNoWinStats()) {
			list.add(r.getString(R.string.noWinStats));
		}
		if (shouldHideComments()) {
			list.add(r.getString(R.string.comments));
		}
		if (shouldHidePlayers()) {
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
		if (PreferencesUtils.editPlayer(this)) {
			addPlayer(new Intent(), REQUEST_ADD_PLAYER);
		} else {
			mPlay.addPlayer(new Player());
			bindUiPlayers();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Player player = new Player(data);
			if (requestCode == REQUEST_ADD_PLAYER) {
				mPlay.addPlayer(player);
			} else {
				mPlay.replacePlayer(player, requestCode - 1);
			}
			bindUiPlayers();
		}
	}

	public void onTimerEnd(final View view) {
		mPlay.end();
		bindUiPlay();
		mLengthView.requestFocus();
	}

	private void bindUi() {
		bindUiPlay();
		bindUiPlayers();
	}

	private void bindUiPlay() {
		changeName(mPlay.GameName);
		setDateButtonText();
		mQuantityView.setText((mPlay.Quantity == Play.QUANTITY_DEFAULT) ? "" : String.valueOf(mPlay.Quantity));
		mLengthView.setText((mPlay.Length == Play.LENGTH_DEFAULT) ? "" : String.valueOf(mPlay.Length));
		UIUtils.startTimerWithSystemTime(mTimer, mPlay.StartTime);
		mLocationView.setText(mPlay.Location);
		mIncompleteView.setChecked(mPlay.Incomplete);
		mNoWinStatsView.setChecked(mPlay.NoWinStats);
		mCommentsView.setText(mPlay.Comments);
		hideFields();
		supportInvalidateOptionsMenu();
	}

	private void bindUiPlayers() {
		mPlayerList.removeAllViews();
		calculatePlayerCount();
		int position = 1;
		for (Player p : mPlay.getPlayers()) {
			PlayerRow row = new PlayerRow(LogPlayActivity.this);
			row.setPlayer(p);
			row.setOnEditListener(onPlayerEdit());
			row.setOnDeleteListener(onPlayerDelete());
			row.setTag(position);
			position++;
			mPlayerList.addView(row);
		}
		hideFields();
	}

	private void calculatePlayerCount() {
		Resources r = getResources();
		int playerCount = mPlay.getPlayers().size();
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
				mPlay.removePlayer(((PlayerRow) v).getPlayer());
				bindUiPlayers();
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
		mTimer = (Chronometer) findViewById(R.id.timer);
		mCommentsView = (EditText) findViewById(R.id.log_play_comments);
		mPlayerLabel = (TextView) findViewById(R.id.log_play_players_label);
		mPlayerList = (LinearLayout) findViewById(R.id.log_play_player_list);
	}

	private void hideFields() {
		findViewById(R.id.log_play_length_label).setVisibility(shouldHideLength() ? View.INVISIBLE : View.VISIBLE);
		mLengthView.setVisibility(shouldHideLength() ? View.INVISIBLE : View.VISIBLE);

		findViewById(R.id.log_play_quantity_label).setVisibility(shouldHideQuantity() ? View.INVISIBLE : View.VISIBLE);
		mQuantityView.setVisibility(shouldHideQuantity() ? View.INVISIBLE : View.VISIBLE);

		findViewById(R.id.log_play_length_quantity_root).setVisibility(
			shouldHideLength() && shouldHideQuantity() ? View.GONE : View.VISIBLE);

		findViewById(R.id.log_play_location_root).setVisibility(shouldHideLocation() ? View.GONE : View.VISIBLE);
		mLocationView.setVisibility(shouldHideLocation() ? View.GONE : View.VISIBLE);

		mIncompleteView.setVisibility(shouldHideIncomplete() ? View.GONE : View.VISIBLE);
		mNoWinStatsView.setVisibility(shouldHideNoWinStats() ? View.GONE : View.VISIBLE);

		findViewById(R.id.timer_root).setVisibility(mPlay.hasStarted() ? View.VISIBLE : View.GONE);

		findViewById(R.id.log_play_comments_label).setVisibility(shouldHideComments() ? View.GONE : View.VISIBLE);
		mCommentsView.setVisibility(shouldHideComments() ? View.GONE : View.VISIBLE);

		mPlayerLabel.setVisibility(shouldHidePlayers() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_players_add).setVisibility(shouldHidePlayers() ? View.GONE : View.VISIBLE);
	}

	private boolean shouldHideQuantity() {
		return !PreferencesUtils.showLogPlayQuantity(this) && !mQuantityShown && !(mPlay.Quantity > 1);
	}

	private boolean shouldHideLength() {
		return !PreferencesUtils.showLogPlayLength(this) && !mLengthShown && !(mPlay.Length > 0);
	}

	private boolean shouldHideLocation() {
		return !PreferencesUtils.showLogPlayLocation(this) && !mLocationShown && TextUtils.isEmpty(mPlay.Location);
	}

	private boolean shouldHideIncomplete() {
		return !PreferencesUtils.showLogPlayIncomplete(this) && !mIncompleteShown && !mPlay.Incomplete;
	}

	private boolean shouldHideNoWinStats() {
		return !PreferencesUtils.showLogPlayNoWinStats(this) && !mNoWinStatsShown && !mPlay.NoWinStats;
	}

	private boolean shouldHideComments() {
		return !PreferencesUtils.showLogPlayComments(this) && !mCommentsShown && TextUtils.isEmpty(mPlay.Comments);
	}

	private boolean shouldHidePlayers() {
		return !PreferencesUtils.showLogPlayPlayerList(this) && !mPlayersShown && (mPlay.getPlayers().size() == 0);
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
		mDateButton.setText(mPlay.getDateForDisplay(this));
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
		// player info already captured
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

				loader.abandon();
				mPlay.fromCursor(cursor);
				if (mEndPlay) {
					mPlay.end();
				}
				bindUiPlay();
				mPlayLoaded = true;
				if (mPlayersLoaded) {
					onQueriesComplete();
				}
				break;
			case PlayerQuery._TOKEN:
				loader.abandon();
				mPlay.clearPlayers();
				while (cursor.moveToNext()) {
					Player player = new Player(cursor);
					mPlay.addPlayer(player);
				}
				bindUiPlayers();
				mPlayersLoaded = true;
				if (mPlayLoaded) {
					onQueriesComplete();
				}
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

	private void onQueriesComplete() {
		mOriginalPlay = new Play(mPlay);
		signalDataLoaded();
		maybeCreateCopy();
	}

	private void maybeCreateCopy() {
		if (ACTION_PLAY_AGAIN.equals(getIntent().getAction())) {
			mPlay.resetForCopy();
			mOriginalPlay = new Play(mPlay);
			bindUi();
			saveDraft(false);
			mDeleteOnCancel = true;
			getIntent().setAction(Intent.ACTION_EDIT);
		}
	}

	private void signalDataLoaded() {
		mPlayLoaded = true;
		mPlayersLoaded = true;
		supportInvalidateOptionsMenu();
	}

	private void changeName(String gameName) {
		if (!TextUtils.isEmpty(gameName)) {
			ActionBar ab = getSupportActionBar();
			if (!gameName.equals(ab.getSubtitle())) {
				ab.setSubtitle(gameName);
			}
		}
	}

	private interface PlayQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays.PLAY_ID, PlayItems.NAME, PlayItems.OBJECT_ID, Plays.DATE, Plays.LOCATION,
			Plays.LENGTH, Plays.QUANTITY, Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.COMMENTS, Plays.START_TIME };
	}

	private interface PlayerQuery {
		int _TOKEN = 0x02;
		String[] PROJECTION = { PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.START_POSITION, PlayPlayers.COLOR,
			PlayPlayers.SCORE, PlayPlayers.RATING, PlayPlayers.NEW, PlayPlayers.WIN, };
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
