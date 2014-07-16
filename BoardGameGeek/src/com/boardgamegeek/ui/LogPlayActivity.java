package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuBuilder.Callback;
import com.actionbarsherlock.internal.view.menu.MenuPopupHelper;
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
import com.boardgamegeek.ui.widget.DatePickerDialogFragment;
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
import com.squareup.picasso.Picasso;

public class LogPlayActivity extends SherlockFragmentActivity implements OnDateSetListener {
	private static final String TAG = makeLogTag(LogPlayActivity.class);

	private static final int HELP_VERSION = 2;
	private static final int REQUEST_ADD_PLAYER = 999;

	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_END_PLAY = "END_PLAY";
	public static final String KEY_PLAY_AGAIN = "PLAY_AGAIN";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_QUANTITY_SHOWN = "QUANTITY_SHOWN";
	private static final String KEY_LENGTH_SHOWN = "LENGTH_SHOWN";
	private static final String KEY_LOCATION_SHOWN = "LOCATION_SHOWN";
	private static final String KEY_INCOMPLETE_SHOWN = "INCOMPLETE_SHOWN";
	private static final String KEY_NO_WIN_STATS_SHOWN = "NO_WIN_STATS_SHOWN";
	private static final String KEY_COMMENTS_SHOWN = "COMMENTS_SHOWN";
	private static final String KEY_PLAYERS_SHOWN = "PLAYERS_SHOWN";
	private static final String KEY_DELETE_ON_CANCEL = "DELETE_ON_CANCEL";
	private static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	private static final String DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG";
	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYERS = 1 << 1;
	private static final int TOKEN_ID = 1 << 2;
	private static final int TOKEN_UNITIALIZED = 1 << 31;
	private static final String[] PLAY_PROJECTION = { Plays.PLAY_ID, PlayItems.NAME, PlayItems.OBJECT_ID, Plays.DATE,
		Plays.LOCATION, Plays.LENGTH, Plays.QUANTITY, Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.COMMENTS,
		Plays.START_TIME };
	private static final String[] PLAYER_PROJECTION = { PlayPlayers.USER_NAME, PlayPlayers.NAME,
		PlayPlayers.START_POSITION, PlayPlayers.COLOR, PlayPlayers.SCORE, PlayPlayers.RATING, PlayPlayers.NEW,
		PlayPlayers.WIN, };
	private static final String[] ID_PROJECTION = { "MAX(plays." + Plays.PLAY_ID + ")" };

	private int mPlayId;
	private int mGameId;
	private String mGameName;
	private boolean mEndPlay;
	private boolean mPlayAgain;
	private String mThumbnailUrl;
	private String mImageUrl;

	private QueryHandler mHandler;
	private int mOutstandingQueries = TOKEN_UNITIALIZED;

	private Play mPlay;
	private Play mOriginalPlay;
	private Random mRandom = new Random();
	private PlayAdapter mPlayAdapter;
	private Builder mAddPlayersBuilder;
	private List<Player> mPlayersToAdd = new ArrayList<Player>();
	private List<String> mUsernames = new ArrayList<String>();
	private List<String> mNames = new ArrayList<String>();

	private TextView mHeaderView;
	private Button mDateButton;
	private DatePickerDialogFragment mDatePickerFragment;
	private EditText mQuantityView;
	private EditText mLengthView;
	private AutoCompleteTextView mLocationView;
	private CheckBox mIncompleteView;
	private CheckBox mNoWinStatsView;
	private Chronometer mTimer;
	private View mTimerToggle;
	private EditText mCommentsView;
	private LinearLayout mPlayerHeader;
	private TextView mPlayerLabel;
	private DragSortListView mPlayerList;
	private Button mAddFieldButton;
	private MenuBuilder mFullPopupMenu;
	private MenuBuilder mShortPopupMenu;

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

	private boolean mLaunchingActivity;
	private boolean mDeleteOnCancel;
	private boolean mSaveOnPause = true;
	private boolean mCustomPlayerSort;

	private final View.OnClickListener mActionBarListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			onActionBarItemSelected(v.getId());
		}
	};

	@SuppressLint("HandlerLeak")
	private class QueryHandler extends AsyncQueryHandler {
		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			// If the query didn't return a cursor for some reason return
			if (cursor == null) {
				return;
			}

			// If the Activity is finishing, then close the cursor
			if (isFinishing()) {
				cursor.close();
				return;
			}

			switch (token) {
				case TOKEN_PLAY:
					if (cursor.getCount() == 0) {
						// The cursor is empty. This can happen if the play was deleted
						cursor.close();
						LogPlayActivity.this.finish();
						return;
					}

					cursor.moveToFirst();
					mPlay = PlayBuilder.fromCursor(cursor);
					cursor.close();
					if (mEndPlay) {
						mPlay.end();
					}
					if ((mOutstandingQueries & TOKEN_PLAYERS) != 0) {
						mHandler.startQuery(TOKEN_PLAYERS, null, Plays.buildPlayerUri(mPlayId), PLAYER_PROJECTION,
							null, null, null);
					}
					setModelIfDone(token);
					break;
				case TOKEN_PLAYERS:
					try {
						mPlay.setPlayers(cursor);
					} finally {
						cursor.close();
					}
					setModelIfDone(token);
					break;
				case TOKEN_ID:
					int id = Play.UNSYNCED_PLAY_ID;
					try {
						int lastId = 0;
						if (cursor.getCount() == 1 && cursor.moveToFirst()) {
							lastId = cursor.getInt(0);
						}
						if (lastId >= id) {
							id = lastId + 1;
						}
					} finally {
						cursor.close();
					}
					mPlayId = id;
					setModelIfDone(token);
					break;
				default:
					cursor.close();
					break;
			}
		}
	}

	private void setModelIfDone(int queryType) {
		synchronized (this) {
			mOutstandingQueries &= ~queryType;
			if (mOutstandingQueries == 0) {
				if (mPlay == null) {
					mPlay = new Play(mPlayId, mGameId, mGameName);
					mPlay.setCurrentDate();

					long lastPlay = PreferencesUtils.getLastPlayTime(this);
					if (DateTimeUtils.howManyHoursOld(lastPlay) < 3) {
						mPlay.location = PreferencesUtils.getLastPlayLocation(this);
						mPlay.setPlayers(PreferencesUtils.getLastPlayPlayers(this));
						mPlay.pickStartPlayer(0);
					}
				}
				if (mPlayAgain) {
					mPlay.playId = mPlayId;
					mPlay = PlayBuilder.playAgain(mPlay);
				}
				mOriginalPlay = PlayBuilder.copy(mPlay);
				finishDataLoad();
			}
		}
	}

	private void finishDataLoad() {
		mOutstandingQueries = 0;
		mCustomPlayerSort = mPlay.arePlayersCustomSorted();
		if (mEndPlay) {
			NotificationUtils.cancel(LogPlayActivity.this, NotificationUtils.ID_PLAY_TIMER);
		}

		bindUi();
		findViewById(R.id.progress).setVisibility(View.GONE);
		findViewById(R.id.form).setVisibility(View.VISIBLE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActivityUtils.setDoneCancelActionBarView(this, mActionBarListener);
		setContentView(R.layout.activity_logplay);
		setUiVariables();
		mHandler = new QueryHandler(getContentResolver());

		final Intent intent = getIntent();
		mPlayId = intent.getIntExtra(KEY_PLAY_ID, BggContract.INVALID_ID);
		mGameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(KEY_GAME_NAME);
		mEndPlay = intent.getBooleanExtra(KEY_END_PLAY, false);
		mPlayAgain = intent.getBooleanExtra(KEY_PLAY_AGAIN, false);
		mThumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL);
		mImageUrl = intent.getStringExtra(KEY_IMAGE_URL);

		if (mGameId <= 0) {
			String message = "Can't log a play without a game ID.";
			LOGW(TAG, message);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			finish();
		}
		if (TextUtils.isEmpty(mGameName)) {
			mHeaderView.setText(getTitle());
		} else {
			mHeaderView.setText(getTitle() + " - " + mGameName);
		}

		if (!TextUtils.isEmpty(mImageUrl)) {
			Picasso.with(this).load(mImageUrl).fit().centerCrop().into((ImageView) findViewById(R.id.thumbnail));
		}

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
		}
		startQuery();

		UIUtils.showHelpDialog(this, HelpUtils.HELP_LOGPLAY_KEY, HELP_VERSION, R.string.help_logplay);
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
		if (mOutstandingQueries == 0) {
			captureForm();
			PlayBuilder.toBundle(mPlay, outState, "P");
			PlayBuilder.toBundle(mOriginalPlay, outState, "O");
		}
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
		if (mSaveOnPause && !mLaunchingActivity) {
			saveDraft(false);
		}
	}

	@Override
	public void onBackPressed() {
		saveDraft(true);
		setResult(Activity.RESULT_OK);
		finish();
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

	private void setUiVariables() {
		mPlayerList = (DragSortListView) findViewById(android.R.id.list);
		View root = View.inflate(this, R.layout.header_logplay, null);
		mPlayerList.addHeaderView(root);
		mPlayAdapter = new PlayAdapter();
		mPlayerList.setAdapter(mPlayAdapter);

		mHeaderView = (TextView) root.findViewById(R.id.header);
		mDateButton = (Button) root.findViewById(R.id.log_play_date);
		mDatePickerFragment = (DatePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(
			DATE_PICKER_DIALOG_TAG);
		if (mDatePickerFragment != null) {
			mDatePickerFragment.setOnDateSetListener(this);
		}
		mQuantityView = (EditText) root.findViewById(R.id.log_play_quantity);
		mLengthView = (EditText) root.findViewById(R.id.log_play_length);
		mLocationView = (AutoCompleteTextView) root.findViewById(R.id.log_play_location);
		mLocationView.setAdapter(new AutoCompleteAdapter(this, Plays.LOCATION, Plays.buildLocationsUri()));
		mIncompleteView = (CheckBox) root.findViewById(R.id.log_play_incomplete);
		mNoWinStatsView = (CheckBox) root.findViewById(R.id.log_play_no_win_stats);
		mTimer = (Chronometer) root.findViewById(R.id.timer);
		mTimerToggle = root.findViewById(R.id.timer_toggle);
		mCommentsView = (EditText) root.findViewById(R.id.log_play_comments);
		mPlayerHeader = (LinearLayout) root.findViewById(R.id.log_play_players_header);
		mPlayerLabel = (TextView) root.findViewById(R.id.log_play_players_label);
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

	private void bindUi() {
		setDateButtonText();
		mQuantityView.setTextKeepState((mPlay.quantity == Play.QUANTITY_DEFAULT) ? "" : String.valueOf(mPlay.quantity));
		bindLength();
		mLocationView.setTextKeepState(mPlay.location);
		mIncompleteView.setChecked(mPlay.Incomplete());
		mNoWinStatsView.setChecked(mPlay.NoWinStats());
		mCommentsView.setTextKeepState(mPlay.comments);
		bindUiPlayers();
	}

	private void bindLength() {
		mLengthView.setTextKeepState((mPlay.length == Play.LENGTH_DEFAULT) ? "" : String.valueOf(mPlay.length));
		UIUtils.startTimerWithSystemTime(mTimer, mPlay.startTime);
	}

	private void bindUiPlayers() {
		// calculate player count
		Resources r = getResources();
		int playerCount = mPlay.getPlayerCount();
		if (playerCount <= 0) {
			mPlayerLabel.setText(r.getString(R.string.title_players));
		} else {
			mPlayerLabel.setText(r.getString(R.string.title_players) + " - " + String.valueOf(playerCount));
		}

		mPlayerList.setDragEnabled(!mCustomPlayerSort);
		mPlayAdapter.notifyDataSetChanged();
		setViewVisibility();
	}

	private void setViewVisibility() {
		boolean enabled = false;
		if (mPlay == null) {
			// all fields should be hidden, so it shouldn't matter
			return;
		}

		enabled |= hideRow(shouldHideLength() && !mPlay.hasStarted(), findViewById(R.id.log_play_length_root));
		if (mPlay.hasStarted()) {
			mLengthView.setVisibility(View.GONE);
			mTimer.setVisibility(View.VISIBLE);
		} else {
			mLengthView.setVisibility(View.VISIBLE);
			mTimer.setVisibility(View.GONE);
		}
		if (mPlay.hasStarted() || DateUtils.isToday(mPlay.getDateInMillis() + mPlay.length * 60 * 1000)) {
			mTimerToggle.setVisibility(View.VISIBLE);
		} else {
			mTimerToggle.setVisibility(View.INVISIBLE);
		}

		enabled |= hideRow(shouldHideQuantity(), findViewById(R.id.log_play_quantity_root));
		enabled |= hideRow(shouldHideLocation(), findViewById(R.id.log_play_location_root));
		enabled |= hideRow(shouldHideIncomplete(), mIncompleteView);
		enabled |= hideRow(shouldHideNoWinStats(), mNoWinStatsView);
		enabled |= hideRow(shouldHideComments(), findViewById(R.id.log_play_comments_root));
		enabled |= hideRow(shouldHidePlayers(), mPlayerHeader);
		findViewById(R.id.clear_players).setEnabled(mPlay.getPlayerCount() > 0);

		findViewById(R.id.add_player).setVisibility(shouldHidePlayers() ? View.GONE : View.VISIBLE);
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

	private void startQuery() {
		if (mPlay != null) {
			// we already have the play from the saved instance
			finishDataLoad();
		} else {
			if (mPlayId > 0) {
				// Editing or copying an existing play, so retrieve it
				mDeleteOnCancel = false;
				mOutstandingQueries = TOKEN_PLAY | TOKEN_PLAYERS;
				if (mPlayAgain) {
					mDeleteOnCancel = true;
					mOutstandingQueries |= TOKEN_ID;
					mHandler.startQuery(TOKEN_ID, null, Plays.CONTENT_SIMPLE_URI, ID_PROJECTION, null, null, null);
				}
				mHandler.startQuery(TOKEN_PLAY, null, Plays.buildPlayUri(mPlayId), PLAY_PROJECTION, null, null, null);
			} else {
				// Starting a new play
				mDeleteOnCancel = true;
				mOutstandingQueries = TOKEN_ID;
				mHandler.startQuery(TOKEN_ID, null, Plays.CONTENT_SIMPLE_URI, ID_PROJECTION, null, null, null);
			}
		}
	}

	private boolean onActionBarItemSelected(int itemId) {
		switch (itemId) {
			case R.id.menu_done:
				if (mPlay == null) {
					cancel();
				} else {
					if (mPlay.hasStarted()) {
						saveDraft(true);
						setResult(Activity.RESULT_OK);
						finish();
					} else {
						logPlay();
					}
				}
				return true;
			case R.id.menu_cancel:
				cancel();
				return true;
		}
		return false;
	}

	private void logPlay() {
		if (save(Play.SYNC_STATUS_PENDING_UPDATE)) {
			if (!mPlay.hasBeenSynced()
				&& DateUtils.isToday(mPlay.getDateInMillis() + Math.max(60, mPlay.length) * 60 * 1000)) {
				PreferencesUtils.putLastPlayTime(this, System.currentTimeMillis());
				PreferencesUtils.putLastPlayLocation(this, mPlay.location);
				PreferencesUtils.putLastPlayPlayers(this, mPlay.getPlayers());
			}
			NotificationUtils.cancel(this, NotificationUtils.ID_PLAY_TIMER);
			triggerUpload();
			Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show();
		}
		setResult(Activity.RESULT_OK);
		finish();
	}

	private void saveDraft(boolean showToast) {
		if (save(Play.SYNC_STATUS_IN_PROGRESS)) {
			if (showToast) {
				Toast.makeText(this, R.string.msg_saving_draft, Toast.LENGTH_SHORT).show();
			}
		}
	}

	private boolean save(int syncStatus) {
		if (mPlay == null) {
			return false;
		}
		mSaveOnPause = false;
		if (syncStatus != Play.SYNC_STATUS_PENDING_DELETE) {
			captureForm();
		}
		mPlay.syncStatus = syncStatus;
		PlayPersister.save(this, mPlay);
		return true;
	}

	private void cancel() {
		mSaveOnPause = false;
		captureForm();
		if (mPlay == null || mPlay.equals(mOriginalPlay)) {
			if (mDeleteOnCancel) {
				if (save(Play.SYNC_STATUS_PENDING_DELETE)) {
					triggerUpload();
				}
			}
			setResult(RESULT_CANCELED);
			finish();
		} else {
			if (mDeleteOnCancel) {
				ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (save(Play.SYNC_STATUS_PENDING_DELETE)) {
								triggerUpload();
							}
							setResult(RESULT_CANCELED);
							finish();
						}
					}).show();
			} else {
				ActivityUtils.createCancelDialog(this).show();
			}
		}
	}

	private void triggerUpload() {
		SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
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
						viewToScroll = mPlayerHeader;
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

	public void addPlayer(View v) {
		if (PreferencesUtils.editPlayer(this)) {
			if (mPlay.getPlayerCount() == 0) {
				if (!showPlayersToAddDialog()) {
					editPlayer(new Intent(), REQUEST_ADD_PLAYER);
				}
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

	private boolean showPlayersToAddDialog() {
		if (mAddPlayersBuilder == null) {
			mAddPlayersBuilder = new AlertDialog.Builder(this).setTitle(R.string.title_add_players)
				.setPositiveButton(android.R.string.ok, addPlayersButtonClickListener())
				.setNeutralButton(R.string.more, addPlayersButtonClickListener())
				.setNegativeButton(android.R.string.cancel, null);
		}

		captureForm(); // to get location

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

		if (descriptions.size() == 0) {
			return false;
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

		return true;
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

	public void onDateClick(View v) {
		if (mDatePickerFragment == null) {
			mDatePickerFragment = new DatePickerDialogFragment();
			mDatePickerFragment.setOnDateSetListener(this);
		}
		final FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.executePendingTransactions();
		mDatePickerFragment.setCurrentDateInMillis(mPlay.getDateInMillis());
		mDatePickerFragment.show(fragmentManager, DATE_PICKER_DIALOG_TAG);
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		if (mPlay != null) {
			mPlay.setDate(year, monthOfYear, dayOfMonth);
			setDateButtonText();
			setViewVisibility();
		}
	}

	private void setDateButtonText() {
		mDateButton.setText(mPlay.getDateForDisplay(this));
	}

	public void onTimer(View v) {
		if (mPlay.hasStarted()) {
			mEndPlay = true;
			mPlay.end();
			bindLength();
			setViewVisibility();
			NotificationUtils.cancel(this, NotificationUtils.ID_PLAY_TIMER);
			if (mPlay.length > 0) {
				mLengthView.setSelection(0, mLengthView.getText().length());
				mLengthView.requestFocus();
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(mLengthView,
					InputMethodManager.SHOW_IMPLICIT);
			}
		} else {
			if (mPlay.length == 0) {
				startTimer();
			} else {
				ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_timer_reset,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							startTimer();
						}
					}).show();
			}
		}
	}

	private void startTimer() {
		mPlay.start();
		bindLength();
		setViewVisibility();
		NotificationUtils.launchStartNotificationWithTicker(this, mPlay, mThumbnailUrl, mImageUrl);
	}

	public void onPlayerSort(View v) {
		MenuPopupHelper popup = null;
		if (!mCustomPlayerSort && mPlay.getPlayerCount() > 1) {
			if (mFullPopupMenu == null) {
				mFullPopupMenu = new MenuBuilder(this);
				MenuItem mi = mFullPopupMenu.add(MenuBuilder.NONE, R.id.menu_custom_player_order, MenuBuilder.NONE,
					R.string.menu_custom_player_order);
				mi.setCheckable(true);
				mFullPopupMenu.add(MenuBuilder.NONE, R.id.menu_pick_start_player, 2, R.string.menu_pick_start_player);
				mFullPopupMenu.add(MenuBuilder.NONE, R.id.menu_random_start_player, 3,
					R.string.menu_random_start_player);
				mFullPopupMenu.add(MenuBuilder.NONE, R.id.menu_random_player_order, 4,
					R.string.menu_random_player_order);
				mFullPopupMenu.setCallback(popupMenuCallback());
			}
			mFullPopupMenu.getItem(0).setChecked(mCustomPlayerSort);
			popup = new MenuPopupHelper(this, mFullPopupMenu, v);
		} else {
			if (mShortPopupMenu == null) {
				mShortPopupMenu = new MenuBuilder(this);
				MenuItem mi = mShortPopupMenu.add(MenuBuilder.NONE, R.id.menu_custom_player_order, MenuBuilder.NONE,
					R.string.menu_custom_player_order);
				mi.setCheckable(true);
				mShortPopupMenu.setCallback(popupMenuCallback());
			}
			mShortPopupMenu.getItem(0).setChecked(mCustomPlayerSort);
			popup = new MenuPopupHelper(this, mShortPopupMenu, v);
		}
		popup.show();
	}

	private Callback popupMenuCallback() {
		return new MenuBuilder.Callback() {
			@Override
			public void onMenuModeChange(MenuBuilder menu) {
			}

			@Override
			public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
				switch (item.getItemId()) {
					case R.id.menu_custom_player_order:
						if (mCustomPlayerSort) {
							if (mPlay.hasStartingPositions() && mPlay.arePlayersCustomSorted()) {
								Dialog dialog = ActivityUtils.createConfirmationDialog(LogPlayActivity.this,
									R.string.are_you_sure_player_sort_custom_off,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											mPlay.pickStartPlayer(0);
											mCustomPlayerSort = !mCustomPlayerSort;
											bindUiPlayers();
										}
									});
								dialog.show();
							} else {
								mPlay.pickStartPlayer(0);
								mCustomPlayerSort = !mCustomPlayerSort;
								bindUiPlayers();
							}
						} else {
							if (mPlay.hasStartingPositions()) {
								AlertDialog.Builder builder = new AlertDialog.Builder(LogPlayActivity.this)
									.setCancelable(true).setTitle(R.string.title_custom_player_order)
									.setMessage(R.string.message_custom_player_order)
									.setNegativeButton(R.string.keep, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											mCustomPlayerSort = !mCustomPlayerSort;
											bindUiPlayers();
										}
									}).setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											mCustomPlayerSort = !mCustomPlayerSort;
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
				}
				return false;
			}
		};
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

	private CharSequence[] createArrayOfPlayerDescriptions() {
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

	public void onClearPlayers(View v) {
		ActivityUtils.createConfirmationDialog(this, R.string.are_you_sure_players_clear,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mPlay.clearPlayers();
					bindUiPlayers();
				}
			}).show();
	}

	private void editPlayer(Intent intent, int requestCode) {
		mLaunchingActivity = true;
		intent.setClass(LogPlayActivity.this, LogPlayerActivity.class);
		intent.putExtra(LogPlayerActivity.KEY_GAME_ID, mPlay.gameId);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, mPlay.gameName);
		intent.putExtra(LogPlayerActivity.KEY_IMAGE_URL, mImageUrl);
		intent.putExtra(LogPlayerActivity.KEY_END_PLAY, mEndPlay);
		if (!mCustomPlayerSort && requestCode == REQUEST_ADD_PLAYER) {
			intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, mPlay.getPlayerCount() + 1);
		}
		startActivityForResult(intent, requestCode);
	}

	/**
	 * Captures the data in the form in the mPlay object
	 */
	private void captureForm() {
		if (mPlay == null) {
			return;
		}
		// date info already captured
		mPlay.quantity = StringUtils.parseInt(mQuantityView.getText().toString().trim(), 1);
		mPlay.length = StringUtils.parseInt(mLengthView.getText().toString().trim());
		mPlay.location = mLocationView.getText().toString().trim();
		mPlay.setIncomplete(mIncompleteView.isChecked());
		mPlay.setNoWinStats(mNoWinStatsView.isChecked());
		mPlay.comments = mCommentsView.getText().toString().trim();
		// player info already captured
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
