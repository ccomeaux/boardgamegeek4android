package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.Calendar;
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
	private List<String> mUsernames = new ArrayList<String>();
	private List<String> mNames = new ArrayList<String>();

	private View mHeaderView;
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
	private Button mAddFieldButton;

	private InputMethodManager mInputMethodManager;
	private boolean mDataLoaded;
	private boolean mPrefShowLocation;
	private boolean mPrefShowLength;
	private boolean mPrefShowQuantity;
	private boolean mPrefShowIncomplete;
	private boolean mPrefShowNoWinStats;
	private boolean mPrefShowComments;
	private boolean mPrefShowPlayers;
	private boolean mUserShowLocation;
	private boolean mUserShowLength;
	private boolean mUserShowQuantity;
	private boolean mUserShowIncomplete;
	private boolean mUserShowNoWinStats;
	private boolean mUserShowComments;
	private boolean mUserShowPlayers;
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
			mUserShowQuantity = savedInstanceState.getBoolean(KEY_QUANTITY_SHOWN);
			mUserShowLength = savedInstanceState.getBoolean(KEY_LENGTH_SHOWN);
			mUserShowLocation = savedInstanceState.getBoolean(KEY_LOCATION_SHOWN);
			mUserShowIncomplete = savedInstanceState.getBoolean(KEY_INCOMPLETE_SHOWN);
			mUserShowNoWinStats = savedInstanceState.getBoolean(KEY_NO_WIN_STATS_SHOWN);
			mUserShowComments = savedInstanceState.getBoolean(KEY_COMMENTS_SHOWN);
			mUserShowPlayers = savedInstanceState.getBoolean(KEY_PLAYERS_SHOWN);
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
				mPlay.playId = BggContract.INVALID_ID;
				mDeleteOnCancel = true;
				mPlay.setCurrentDate();

				long lastPlay = PreferencesUtils.getLastPlayTime(this);
				if (DateTimeUtils.howManyHoursOld(lastPlay) < 3) {
					mPlay.location = PreferencesUtils.getLastPlayLocation(this);
					mPlay.setPlayers(PreferencesUtils.getLastPlayPlayers(this));
					mPlay.pickStartPlayer(0);
					bindUiPlay(); // needed for location to be saved in draft
				}

				saveDraft(false);
				setResult(mPlay.playId);
				mOriginalPlay = PlayBuilder.copy(mPlay);
				signalDataLoaded();

				if (TextUtils.isEmpty(mPlay.location)) {
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
		mPrefShowLocation = PreferencesUtils.showLogPlayLocation(this);
		mPrefShowLength = PreferencesUtils.showLogPlayLength(this);
		mPrefShowQuantity = PreferencesUtils.showLogPlayQuantity(this);
		mPrefShowIncomplete = PreferencesUtils.showLogPlayIncomplete(this);
		mPrefShowNoWinStats = PreferencesUtils.showLogPlayNoWinStats(this);
		mPrefShowComments = PreferencesUtils.showLogPlayComments(this);
		mPrefShowPlayers = PreferencesUtils.showLogPlayPlayerList(this);
		setViewVisibility();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		PlayBuilder.toBundle(mPlay, outState, "P");
		PlayBuilder.toBundle(mOriginalPlay, outState, "O");
		outState.putBoolean(KEY_QUANTITY_SHOWN, mUserShowQuantity);
		outState.putBoolean(KEY_LENGTH_SHOWN, mUserShowLength);
		outState.putBoolean(KEY_LOCATION_SHOWN, mUserShowLocation);
		outState.putBoolean(KEY_INCOMPLETE_SHOWN, mUserShowIncomplete);
		outState.putBoolean(KEY_NO_WIN_STATS_SHOWN, mUserShowNoWinStats);
		outState.putBoolean(KEY_COMMENTS_SHOWN, mUserShowComments);
		outState.putBoolean(KEY_PLAYERS_SHOWN, mUserShowPlayers);
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

	public void addField(View v) {
		final CharSequence[] array = createAddFieldArray();
		if (array == null || array.length == 0) {
			return;
		}
		new AlertDialog.Builder(this).setTitle(R.string.add_field)
			.setItems(array, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					View viewToFocus = null;
					View viewToScroll = null;
					Resources r = getResources();
					String selection = array[which].toString();

					if (selection == r.getString(R.string.location)) {
						mUserShowLocation = true;
						viewToFocus = mLocationView;
						viewToScroll = findViewById(R.id.log_play_location_root);
					} else if (selection == r.getString(R.string.length)) {
						mUserShowLength = true;
						viewToFocus = mLengthView;
						viewToScroll = findViewById(R.id.log_play_length_root);
					} else if (selection == r.getString(R.string.quantity)) {
						mUserShowQuantity = true;
						viewToFocus = mQuantityView;
						viewToScroll = findViewById(R.id.log_play_quantity_root);
					} else if (selection == r.getString(R.string.incomplete)) {
						mUserShowIncomplete = true;
						mIncompleteView.setChecked(true);
						viewToScroll = mIncompleteView;
					} else if (selection == r.getString(R.string.noWinStats)) {
						mUserShowNoWinStats = true;
						mNoWinStatsView.setChecked(true);
						viewToScroll = mNoWinStatsView;
					} else if (selection == r.getString(R.string.comments)) {
						mUserShowComments = true;
						viewToFocus = mCommentsView;
						viewToScroll = mCommentsView;
					} else if (selection == r.getString(R.string.title_players)) {
						mUserShowPlayers = true;
						viewToScroll = mPlayerLabel;
					}
					setViewVisibility();
					supportInvalidateOptionsMenu();
					if (viewToFocus != null) {
						viewToFocus.requestFocus();
					}
					// TODO These views are the header, so this doesn't work as expected
					if (viewToScroll != null) {
						final View finalView = viewToScroll;
						mPlayerList.post(new Runnable() {
							@Override
							public void run() {
								mPlayerList.smoothScrollBy(finalView.getBottom(), android.R.integer.config_longAnimTime);
							}
						});
					}
				}
			}).show();
	}

	private void logPlay() {
		save(Play.SYNC_STATUS_PENDING_UPDATE);
		if (!mPlay.hasBeenSynced()) {
			PreferencesUtils.putLastPlayTime(this, System.currentTimeMillis());
			PreferencesUtils.putLastPlayLocation(this, mPlay.location);
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
		mPlay.syncStatus = syncStatus;
		PlayPersister.save(this, mPlay);
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
		bundle.putLong(DatePickerFragment.KEY_DATE, mPlay.getDateInMillis());
		fragment.setArguments(bundle);
		fragment.show(getSupportFragmentManager(), "datePicker");
	}

	public void onAddPlayerClick(View v) {
		if (PreferencesUtils.editPlayer(this)) {
			if (mPlay.getPlayerCount() == 0) {
				showPlayersToAddDialog();
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

	private void showPlayersToAddDialog() {
		if (mAddPlayersBuilder == null) {
			mAddPlayersBuilder = new AlertDialog.Builder(this).setTitle(R.string.title_add_players)
				.setPositiveButton(android.R.string.ok, addPlayersButtonClickListener())
				.setNeutralButton(R.string.more, addPlayersButtonClickListener())
				.setNegativeButton(android.R.string.cancel, null);
		}

		captureForm();

		mPlayersToAdd.clear();
		mUsernames.clear();
		mNames.clear();
		List<String> descriptions = new ArrayList<String>();

		String selection = null;
		String[] selectionArgs = null;
		if (!TextUtils.isEmpty(mPlay.location)) {
			selection = Plays.LOCATION + "=?";
			selectionArgs = new String[] { mPlay.location };
		}
		Cursor cursor = getContentResolver().query(
			Plays.buildPlayersByUniqueNameUri(),
			new String[] { PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.DESCRIPTION,
				PlayPlayers.COUNT, PlayPlayers.UNIQUE_NAME }, selection, selectionArgs, PlayPlayers.SORT_BY_COUNT);
		try {
			while (cursor.moveToNext()) {
				mUsernames.add(cursor.getString(1));
				mNames.add(cursor.getString(2));
				descriptions.add(cursor.getString(3));
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		CharSequence[] array = {};
		mAddPlayersBuilder
			.setMultiChoiceItems(descriptions.toArray(array), null, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Player player = new Player();
					player.username = mUsernames.get(which);
					player.name = mNames.get(which);
					if (isChecked) {
						mPlayersToAdd.add(player);
					} else {
						mPlayersToAdd.remove(player);
					}
				}
			}).create().show();
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
		changeName(mPlay.gameName);
		setDateButtonText();
		mQuantityView.setText((mPlay.quantity == Play.QUANTITY_DEFAULT) ? "" : String.valueOf(mPlay.quantity));
		mLengthView.setText((mPlay.length == Play.LENGTH_DEFAULT) ? "" : String.valueOf(mPlay.length));
		UIUtils.startTimerWithSystemTime(mTimer, mPlay.startTime);
		mLocationView.setText(mPlay.location);
		mIncompleteView.setChecked(mPlay.Incomplete());
		mNoWinStatsView.setChecked(mPlay.NoWinStats());
		mCommentsView.setText(mPlay.comments);
		setViewVisibility();
		supportInvalidateOptionsMenu();
	}

	private void bindUiPlayers() {
		calculatePlayerCount();
		setViewVisibility();
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
		intent.putExtra(LogPlayerActivity.KEY_GAME_ID, mPlay.gameId);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, mPlay.gameName);
		intent.putExtra(LogPlayerActivity.KEY_END_PLAY, mEndPlay);
		if (!mCustomPlayerSort && requestCode == REQUEST_ADD_PLAYER) {
			intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, mPlay.getPlayerCount() + 1);
		}
		startActivityForResult(intent, requestCode);
	}

	private void setUiVariables() {
		mPlayerList = (DragSortListView) findViewById(android.R.id.list);
		mHeaderView = View.inflate(this, R.layout.header_logplay, null);
		mPlayerList.addHeaderView(mHeaderView);
		mPlayerList.setAdapter(mPlayAdapter);

		mDateButton = (Button) mHeaderView.findViewById(R.id.log_play_date);
		mQuantityView = (EditText) mHeaderView.findViewById(R.id.log_play_quantity);
		mLengthView = (EditText) mHeaderView.findViewById(R.id.log_play_length);
		mLocationView = (AutoCompleteTextView) mHeaderView.findViewById(R.id.log_play_location);
		mIncompleteView = (CheckBox) mHeaderView.findViewById(R.id.log_play_incomplete);
		mNoWinStatsView = (CheckBox) mHeaderView.findViewById(R.id.log_play_no_win_stats);
		mTimer = (Chronometer) mHeaderView.findViewById(R.id.timer);
		mCommentsView = (EditText) mHeaderView.findViewById(R.id.log_play_comments);
		mPlayerLabel = (TextView) mHeaderView.findViewById(R.id.log_play_players_label);
		mAddFieldButton = (Button) findViewById(R.id.add_field);

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

	private void setViewVisibility() {
		boolean enabled = false;
		enabled |= hideRow(shouldHideLength() && !mPlay.hasStarted(), findViewById(R.id.log_play_length_root));
		mLengthView.setVisibility(shouldHideLength() ? View.INVISIBLE : View.VISIBLE);
		findViewById(R.id.timer_root).setVisibility(mPlay.hasStarted() ? View.VISIBLE : View.GONE);

		enabled |= hideRow(shouldHideQuantity(), findViewById(R.id.log_play_quantity_root));
		enabled |= hideRow(shouldHideLocation(), findViewById(R.id.log_play_location_root));
		enabled |= hideRow(shouldHideIncomplete(), mIncompleteView);
		enabled |= hideRow(shouldHideNoWinStats(), mNoWinStatsView);
		enabled |= hideRow(shouldHideComments(), findViewById(R.id.log_play_comments_root));
		enabled |= hideRow(shouldHidePlayers(), mPlayerLabel);
		addFooter(!shouldHidePlayers());

		mAddFieldButton.setEnabled(enabled);
	}

	private boolean hideRow(boolean shouldHide, View view) {
		if (shouldHide) {
			view.setVisibility(View.GONE);
			return true;
		}
		view.setVisibility(View.VISIBLE);
		return false;
	}

	private boolean shouldHideLocation() {
		return !mPrefShowLocation && !mUserShowLocation && TextUtils.isEmpty(mPlay.location);
	}

	private boolean shouldHideLength() {
		return !mPrefShowLength && !mUserShowLength && !(mPlay.length > 0);
	}

	private boolean shouldHideQuantity() {
		return !mPrefShowQuantity && !mUserShowQuantity && !(mPlay.quantity > 1);
	}

	private boolean shouldHideIncomplete() {
		return !mPrefShowIncomplete && !mUserShowIncomplete && !mPlay.Incomplete();
	}

	private boolean shouldHideNoWinStats() {
		return !mPrefShowNoWinStats && !mUserShowNoWinStats && !mPlay.NoWinStats();
	}

	private boolean shouldHideComments() {
		return !mPrefShowComments && !mUserShowComments && TextUtils.isEmpty(mPlay.comments);
	}

	private boolean shouldHidePlayers() {
		return !mPrefShowPlayers && !mUserShowPlayers && (mPlay.getPlayerCount() == 0);
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
		mPlay.quantity = StringUtils.parseInt(mQuantityView.getText().toString().trim(), 1);
		mPlay.length = StringUtils.parseInt(mLengthView.getText().toString().trim());
		mPlay.location = mLocationView.getText().toString().trim();
		mPlay.setIncomplete(mIncompleteView.isChecked());
		mPlay.setNoWinStats(mNoWinStatsView.isChecked());
		mPlay.comments = mCommentsView.getText().toString().trim();
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
			setResult(mPlay.playId);
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
		public static final String KEY_DATE = "YEAR";

		private OnDateSetListener mListener;

		public DatePickerFragment(DatePickerDialog.OnDateSetListener listener) {
			mListener = listener;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle b = getArguments();
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(b.getLong(KEY_DATE));
			return new DatePickerDialog(getActivity(), mListener, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DAY_OF_MONTH));
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
