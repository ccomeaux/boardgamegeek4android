package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
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
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
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
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AutoCompleteAdapter;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;

public class LogPlayActivity extends SherlockFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(LogPlayActivity.class);

	private static final int HELP_VERSION = 2;
	private static final int REQUEST_ADD_PLAYER = 999;

	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_END_PLAY = "END_PLAY";
	public static final String KEY_PLAY_AGAIN = "PLAY_AGAIN";
	private static final String KEY_QUANTITY_SHOWN = "QUANTITY_SHOWN";
	private static final String KEY_LENGTH_SHOWN = "LENGTH_SHOWN";
	private static final String KEY_LOCATION_SHOWN = "LOCATION_SHOWN";
	private static final String KEY_INCOMPLETE_SHOWN = "INCOMPLETE_SHOWN";
	private static final String KEY_NO_WIN_STATS_SHOWN = "NO_WIN_STATS_SHOWN";
	private static final String KEY_COMMENTS_SHOWN = "COMMENTS_SHOWN";
	private static final String KEY_PLAYERS_SHOWN = "PLAYERS_SHOWN";
	private static final String KEY_DELETE_ON_CANCEL = "DELETE_ON_CANCEL";
	private static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";

	private Play mPlay;
	private Play mOriginalPlay;
	private boolean mLaunchingActivity;
	private Random mRandom = new Random();
	private PlayAdapter mPlayAdapter;
	private Builder mAddPlayersBuilder;
	private List<Player> mPlayersToAdd = new ArrayList<Player>();

	private Button mDateButton;
	private EditText mQuantityView;
	private EditText mLengthView;
	private AutoCompleteTextView mLocationView;
	private CheckBox mIncompleteView;
	private CheckBox mNoWinStatsView;
	private Chronometer mTimer;
	private EditText mCommentsView;
	private TextView mPlayerLabel;
	private DragSortListView mPlayerList;

	private InputMethodManager mInputMethodManager;
	private boolean mDataLoaded;
	private boolean mQuantityShown;
	private boolean mLengthShown;
	private boolean mLocationShown;
	private boolean mIncompleteShown;
	private boolean mNoWinStatsShown;
	private boolean mCommentsShown;
	private boolean mPlayersShown;
	private boolean mDeleteOnCancel;
	private boolean mEndPlay;
	private boolean mPlayAgain;
	private boolean mCustomPlayerSort;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		if (!Intent.ACTION_EDIT.equals(intent.getAction())) {
			LOGW(TAG, "Received bad intent action: " + intent.getAction());
			finish();
		}

		setContentView(R.layout.activity_logplay);
		getSupportActionBar().setHomeButtonEnabled(false);
		mPlayAdapter = new PlayAdapter();
		setUiVariables();
		mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

		int playId = intent.getIntExtra(KEY_PLAY_ID, BggContract.INVALID_ID);
		int gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		String gameName = intent.getStringExtra(KEY_GAME_NAME);
		mEndPlay = intent.getBooleanExtra(KEY_END_PLAY, false);
		mPlayAgain = intent.getBooleanExtra(KEY_PLAY_AGAIN, false);

		if (gameId <= 0) {
			LOGW(TAG, "Can't log a play without a game ID.");
			Toast.makeText(this, "Can't log a play without a game ID.", Toast.LENGTH_LONG).show();
			finish();
		}
		changeName(gameName);

		boolean requestLocationFocus = false;
		if (savedInstanceState != null) {
			mPlay = PlayBuilder.fromBundle(savedInstanceState, "P");
			mOriginalPlay = PlayBuilder.fromBundle(savedInstanceState, "O");
			mQuantityShown = savedInstanceState.getBoolean(KEY_QUANTITY_SHOWN);
			mLengthShown = savedInstanceState.getBoolean(KEY_LENGTH_SHOWN);
			mLocationShown = savedInstanceState.getBoolean(KEY_LOCATION_SHOWN);
			mIncompleteShown = savedInstanceState.getBoolean(KEY_INCOMPLETE_SHOWN);
			mNoWinStatsShown = savedInstanceState.getBoolean(KEY_NO_WIN_STATS_SHOWN);
			mCommentsShown = savedInstanceState.getBoolean(KEY_COMMENTS_SHOWN);
			mPlayersShown = savedInstanceState.getBoolean(KEY_PLAYERS_SHOWN);
			mDeleteOnCancel = savedInstanceState.getBoolean(KEY_DELETE_ON_CANCEL);
			mCustomPlayerSort = savedInstanceState.getBoolean(KEY_CUSTOM_PLAYER_SORT);
			signalDataLoaded();
		} else {
			mPlay = new Play(playId, gameId, gameName);
			if (playId > 0) {
				// Editing an existing play
				mDeleteOnCancel = false;
				getSupportLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);
			} else {
				// Starting a new play
				mPlay.PlayId = BggContract.INVALID_ID;
				mDeleteOnCancel = true;

				long lastPlay = PreferencesUtils.getLastPlayTime(this);
				if (DateTimeUtils.howManyHoursOld(lastPlay) < 3) {
					mPlay.Location = PreferencesUtils.getLastPlayLocation(this);
					mPlay.setPlayers(PreferencesUtils.getLastPlayPlayers(this));
					mPlay.pickStartPlayer(0);
					bindUiPlay(); // needed for location to be saved in draft
				}

				saveDraft(false);
				setResult(mPlay.PlayId);
				mOriginalPlay = PlayBuilder.copy(mPlay);
				signalDataLoaded();

				if (TextUtils.isEmpty(mPlay.Location)) {
					requestLocationFocus = true;
				}
			}
		}

		bindUi();
		if (requestLocationFocus && mLocationView.getVisibility() == View.VISIBLE) {
			mLocationView.requestFocus();
		}

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
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		PlayBuilder.toBundle(mPlay, outState, "P");
		PlayBuilder.toBundle(mOriginalPlay, outState, "O");
		outState.putBoolean(KEY_QUANTITY_SHOWN, mQuantityShown);
		outState.putBoolean(KEY_LENGTH_SHOWN, mLengthShown);
		outState.putBoolean(KEY_LOCATION_SHOWN, mLocationShown);
		outState.putBoolean(KEY_INCOMPLETE_SHOWN, mIncompleteShown);
		outState.putBoolean(KEY_NO_WIN_STATS_SHOWN, mNoWinStatsShown);
		outState.putBoolean(KEY_COMMENTS_SHOWN, mCommentsShown);
		outState.putBoolean(KEY_PLAYERS_SHOWN, mPlayersShown);
		outState.putBoolean(KEY_DELETE_ON_CANCEL, mDeleteOnCancel);
		outState.putBoolean(KEY_CUSTOM_PLAYER_SORT, mCustomPlayerSort);
	}

	@Override
	protected void onPause() {
		super.onPause();
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
		menu.findItem(R.id.menu_custom_player_order).setChecked(mCustomPlayerSort);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mDataLoaded) {
			menu.findItem(R.id.menu_send).setVisible(!mPlay.hasStarted());
			menu.findItem(R.id.menu_start).setVisible(!mPlay.hasStarted() && !mPlay.hasEnded());
			menu.findItem(R.id.menu_add_field).setVisible(
				shouldHideQuantity() || shouldHideLength() || shouldHideLocation() || shouldHideNoWinStats()
					|| shouldHideIncomplete() || shouldHideComments() || shouldHidePlayers());
			menu.findItem(R.id.menu_player_order).setVisible(!shouldHidePlayers());
			menu.findItem(R.id.menu_custom_player_order).setVisible(true);
			menu.findItem(R.id.menu_pick_start_player).setVisible(mPlay.getPlayerCount() > 1);
			menu.findItem(R.id.menu_pick_start_player).setEnabled(!mCustomPlayerSort);
			menu.findItem(R.id.menu_random_start_player).setVisible(mPlay.getPlayerCount() > 1);
			menu.findItem(R.id.menu_random_start_player).setEnabled(!mCustomPlayerSort);
			menu.findItem(R.id.menu_random_player_order).setVisible(mPlay.getPlayerCount() > 1);
			menu.findItem(R.id.menu_random_player_order).setEnabled(!mCustomPlayerSort);
			menu.findItem(R.id.menu_players_clear).setVisible(mPlay.getPlayerCount() > 1);
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
				NotificationUtils.launchStartNotificationWithTicker(this, mPlay);
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
			case R.id.menu_custom_player_order:
				final MenuItem finalItem = item;
				if (mCustomPlayerSort) {
					if (mPlay.arePlayersCustomSorted()) {
						Dialog dialog = ActivityUtils.createConfirmationDialog(this,
							R.string.are_you_sure_player_sort_custom_off, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mPlay.pickStartPlayer(0);
									bindUiPlayers();
									toggleCustomSort(finalItem);
								}
							});
						dialog.show();
					} else {
						mPlay.pickStartPlayer(0);
						bindUiPlayers();
						toggleCustomSort(item);
					}
				} else {
					toggleCustomSort(item);
					if (mPlay.hasStartingPositions()) {
						AlertDialog.Builder builder = new AlertDialog.Builder(this).setCancelable(false)
							.setTitle(R.string.title_custom_player_order)
							.setMessage(R.string.message_custom_player_order).setNegativeButton(R.string.keep, null)
							.setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mPlay.clearPlayerPositions();
									bindUiPlayers();
								}
							});
						builder = ActivityUtils.addAlertIcon(builder);
						builder.create().show();
					}
				}
				return true;
			case R.id.menu_pick_start_player:
				promptPickStartPlayer();
				return true;
			case R.id.menu_random_start_player:
				int newSeat = mRandom.nextInt(mPlay.getPlayerCount());
				mPlay.pickStartPlayer(newSeat);
				notifyStartPlayer();
				bindUiPlayers();
				return true;
			case R.id.menu_random_player_order:
				mPlay.randomizePlayerOrder();
				notifyStartPlayer();
				bindUiPlayers();
				return true;
			case R.id.menu_players_clear:
				ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_player_sort_custom_off,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mPlay.clearPlayers();
							bindUiPlayers();
						}
					}).show();
				return true;
		}
		return false;
	}

	private void toggleCustomSort(MenuItem item) {
		mCustomPlayerSort = !mCustomPlayerSort;
		item.setChecked(mCustomPlayerSort);
		bindUiPlayers();
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
		for (int i = 0; i < mPlay.getPlayerCount(); i++) {
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
					} else if (selection == r.getString(R.string.title_players)) {
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
		if (!mPlay.hasBeenSynced()) {
			PreferencesUtils.putLastPlayTime(this, System.currentTimeMillis());
			PreferencesUtils.putLastPlayLocation(this, mPlay.Location);
			PreferencesUtils.putLastPlayPlayers(this, mPlay.getPlayers());
		}
		NotificationUtils.cancel(this, NotificationUtils.ID_PLAY_TIMER);
		triggerUpload();
		Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show();
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
				setResult(RESULT_OK);
			}
			triggerUpload();
			finish();
		} else {
			if (mDeleteOnCancel) {
				ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							save(Play.SYNC_STATUS_PENDING_DELETE);
							setResult(RESULT_OK);
							triggerUpload();
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
		if (shouldHideLocation()) {
			list.add(r.getString(R.string.location));
		}
		if (shouldHideLength()) {
			list.add(r.getString(R.string.length));
		}
		if (shouldHideQuantity()) {
			list.add(r.getString(R.string.quantity));
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
			list.add(r.getString(R.string.title_players));
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
			if (mPlay.getPlayerCount() == 0) {
				if (mAddPlayersBuilder == null) {
					mAddPlayersBuilder = new AlertDialog.Builder(this).setTitle(R.string.title_add_players)
						.setPositiveButton(android.R.string.ok, addPlayersButtonClickListener())
						.setNeutralButton(R.string.more, addPlayersButtonClickListener())
						.setNegativeButton(android.R.string.cancel, null);
				}

				captureForm();
				String selection = null;
				String[] selectionArgs = null;
				if (!TextUtils.isEmpty(mPlay.Location)) {
					selection = Plays.LOCATION + "=?";
					selectionArgs = new String[] { mPlay.Location };
				}

				mPlayersToAdd.clear();

				mAddPlayersBuilder
					.setMultiChoiceItems(
						getContentResolver().query(
							Plays.buildPlayersByUniqueNameUri(),
							new String[] { PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME,
								PlayPlayers.CHECKED, PlayPlayers.DESCRIPTION, PlayPlayers.COUNT,
								PlayPlayers.UNIQUE_NAME }, selection, selectionArgs, PlayPlayers.SORT_BY_COUNT),
						PlayPlayers.CHECKED, PlayPlayers.DESCRIPTION, addPlayersMultiChoiceClickListener()).create()
					.show();
			} else {
				editPlayer(new Intent(), REQUEST_ADD_PLAYER);
			}
		} else {
			Player player = new Player();
			if (!mCustomPlayerSort) {
				player.setSeat(mPlay.getPlayerCount() + 1);
			}
			mPlay.addPlayer(player);
			bindUiPlayers();
			mPlayerList.smoothScrollToPosition(mPlayerList.getCount());
		}
	}

	private DialogInterface.OnMultiChoiceClickListener addPlayersMultiChoiceClickListener() {
		return new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				Cursor cursor = (Cursor) ((AlertDialog) dialog).getListView().getAdapter().getItem(which);
				Player player = new Player();
				player.Username = cursor.getString(1);
				player.Name = cursor.getString(2);
				if (isChecked) {
					mPlayersToAdd.add(player);
				} else {
					mPlayersToAdd.remove(player);
				}
			}
		};
	}

	private DialogInterface.OnClickListener addPlayersButtonClickListener() {
		return new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mPlay.setPlayers(mPlayersToAdd);
				if (!mCustomPlayerSort) {
					mPlay.pickStartPlayer(0);
				}
				bindUiPlayers();
				if (which == DialogInterface.BUTTON_NEUTRAL) {
					editPlayer(new Intent(), REQUEST_ADD_PLAYER);
				}
			}
		};
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Player player = data.getParcelableExtra(LogPlayerActivity.KEY_PLAYER);
			if (requestCode == REQUEST_ADD_PLAYER) {
				mPlay.addPlayer(player);
				// prompt for another player
				Intent intent = new Intent();
				intent.putExtra(LogPlayerActivity.KEY_CANCEL_ON_BACK, true);
				editPlayer(intent, REQUEST_ADD_PLAYER);
			} else {
				mPlay.replacePlayer(player, requestCode);
			}
			bindUiPlayers();
		}
	}

	public void onTimerEnd(final View view) {
		mEndPlay = true;
		mPlay.end();
		bindUiPlay();
		if (mLengthView.getVisibility() == View.VISIBLE) {
			mLengthView.setSelection(0, mLengthView.getText().length());
			mLengthView.requestFocus();
			mInputMethodManager.showSoftInput(mLengthView, InputMethodManager.SHOW_IMPLICIT);
		}
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
		calculatePlayerCount();
		hideFields();
		mPlayAdapter.notifyDataSetChanged();
	}

	private void calculatePlayerCount() {
		Resources r = getResources();
		int playerCount = mPlay.getPlayerCount();
		if (playerCount <= 0) {
			mPlayerLabel.setText(r.getString(R.string.title_players));
		} else {
			mPlayerLabel.setText(r.getString(R.string.title_players) + " - " + String.valueOf(playerCount));
		}
	}

	private void editPlayer(Intent intent, int requestCode) {
		mLaunchingActivity = true;
		intent.setClass(LogPlayActivity.this, LogPlayerActivity.class);
		intent.putExtra(LogPlayerActivity.KEY_GAME_ID, mPlay.GameId);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, mPlay.GameName);
		intent.putExtra(LogPlayerActivity.KEY_END_PLAY, mEndPlay);
		if (!mCustomPlayerSort && requestCode == REQUEST_ADD_PLAYER) {
			intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, mPlay.getPlayerCount() + 1);
		}
		startActivityForResult(intent, requestCode);
	}

	private void setUiVariables() {
		mPlayerList = (DragSortListView) findViewById(android.R.id.list);
		View header = View.inflate(this, R.layout.header_logplay, null);
		mPlayerList.addHeaderView(header);
		mPlayerList.setAdapter(mPlayAdapter);

		mDateButton = (Button) header.findViewById(R.id.log_play_date);
		mQuantityView = (EditText) header.findViewById(R.id.log_play_quantity);
		mLengthView = (EditText) header.findViewById(R.id.log_play_length);
		mLocationView = (AutoCompleteTextView) header.findViewById(R.id.log_play_location);
		mIncompleteView = (CheckBox) header.findViewById(R.id.log_play_incomplete);
		mNoWinStatsView = (CheckBox) header.findViewById(R.id.log_play_no_win_stats);
		mTimer = (Chronometer) header.findViewById(R.id.timer);
		mCommentsView = (EditText) header.findViewById(R.id.log_play_comments);
		mPlayerLabel = (TextView) header.findViewById(R.id.log_play_players_label);

		mPlayerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int offsetPosition = position - 1; // offset by the list header
				Player player = (Player) mPlayAdapter.getItem(offsetPosition);
				Intent intent = new Intent();
				intent.putExtra(LogPlayerActivity.KEY_PLAYER, player);
				intent.putExtra(LogPlayerActivity.KEY_END_PLAY, mEndPlay);
				if (!mCustomPlayerSort) {
					intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, player.getSeat());
				}
				editPlayer(intent, offsetPosition);
			}
		});

		mPlayerList.setItemsCanFocus(true);
	}

	private void hideFields() {
		findViewById(R.id.log_play_length_root).setVisibility(
			shouldHideLength() && !mPlay.hasStarted() ? View.GONE : View.VISIBLE);
		mLengthView.setVisibility(shouldHideLength() ? View.INVISIBLE : View.VISIBLE);
		findViewById(R.id.timer_root).setVisibility(mPlay.hasStarted() ? View.VISIBLE : View.GONE);

		findViewById(R.id.log_play_quantity_root).setVisibility(shouldHideQuantity() ? View.GONE : View.VISIBLE);

		findViewById(R.id.log_play_location_root).setVisibility(shouldHideLocation() ? View.GONE : View.VISIBLE);
		mLocationView.setVisibility(shouldHideLocation() ? View.GONE : View.VISIBLE);

		mIncompleteView.setVisibility(shouldHideIncomplete() ? View.GONE : View.VISIBLE);
		mNoWinStatsView.setVisibility(shouldHideNoWinStats() ? View.GONE : View.VISIBLE);

		findViewById(R.id.log_play_comments_label).setVisibility(shouldHideComments() ? View.GONE : View.VISIBLE);
		mCommentsView.setVisibility(shouldHideComments() ? View.GONE : View.VISIBLE);

		mPlayerLabel.setVisibility(shouldHidePlayers() ? View.GONE : View.VISIBLE);
		addFooter(!shouldHidePlayers());
	}

	private void addFooter(boolean add) {
		View footer = View.inflate(this, R.layout.footer_logplay, null);
		if (add && mPlayerList.getFooterViewsCount() == 0) {
			mPlayerList.addFooterView(footer);
			footer.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onAddPlayerClick(v);
				}
			});
		} else if (!add && mPlayerList.getFooterViewsCount() > 0) {
			mPlayerList.removeFooterView(footer);
		}
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
		return !PreferencesUtils.showLogPlayPlayerList(this) && !mPlayersShown && (mPlay.getPlayerCount() == 0);
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

				mPlay = PlayBuilder.fromCursor(cursor);
				if (mEndPlay) {
					mPlay.end();
				}
				bindUiPlay();
				getSupportLoaderManager().restartLoader(PlayerQuery._TOKEN, null, this);
				getSupportLoaderManager().destroyLoader(PlayQuery._TOKEN);
				break;
			case PlayerQuery._TOKEN:
				mPlay.setPlayers(cursor);
				bindUiPlayers();
				getSupportLoaderManager().destroyLoader(PlayerQuery._TOKEN);
				onQueriesComplete();
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
		mOriginalPlay = PlayBuilder.copy(mPlay);
		maybeCreateCopy();
		mCustomPlayerSort = mPlay.arePlayersCustomSorted();
		signalDataLoaded();
	}

	private void maybeCreateCopy() {
		if (mPlayAgain) {
			mPlayAgain = false;
			mPlay = PlayBuilder.playAgain(mPlay);
			mOriginalPlay = PlayBuilder.copy(mPlay);
			bindUi();
			saveDraft(false);
			setResult(mPlay.PlayId);
			mDeleteOnCancel = true;
		}
	}

	private void signalDataLoaded() {
		mDataLoaded = true;
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
	private static class DatePickerFragment extends DialogFragment {
		public static final String KEY_YEAR = "YEAR";
		public static final String KEY_MONTH = "MONTH";
		public static final String KEY_DAY = "DAY";

		private OnDateSetListener mListener;

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

	private class PlayAdapter extends BaseAdapter implements DropListener {
		@Override
		public int getCount() {
			return mPlay == null ? 0 : mPlay.getPlayerCount();
		}

		@Override
		public Object getItem(int position) {
			return mPlay == null ? null : mPlay.getPlayers().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new PlayerRow(LogPlayActivity.this);
				// HACK workaround to make row take up full width
				convertView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			}
			PlayerRow row = (PlayerRow) convertView;
			row.setAutoSort(!mCustomPlayerSort);
			row.setPlayer((Player) getItem(position));
			row.setOnDeleteListener(new PlayerDeleteClickListener(position));
			return convertView;
		}

		@Override
		public void drop(int from, int to) {
			if (!mPlay.reorderPlayers(from + 1, to + 1)) {
				Toast.makeText(LogPlayActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
			}
			notifyDataSetChanged();
		}
	}

	private class PlayerDeleteClickListener implements View.OnClickListener {
		private int mPosition;

		public PlayerDeleteClickListener(int position) {
			mPosition = position;
		}

		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(LogPlayActivity.this);
			builder.setTitle(R.string.are_you_sure_title).setMessage(R.string.are_you_sure_delete_player)
				.setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Player player = (Player) mPlayAdapter.getItem(mPosition);
						Toast.makeText(LogPlayActivity.this, R.string.msg_player_deleted, Toast.LENGTH_SHORT).show();
						mPlay.removePlayer(player, !mCustomPlayerSort);
						bindUiPlayers();
					}
				}).setNegativeButton(R.string.no, null);
			builder.create().show();
		}
	}
}
