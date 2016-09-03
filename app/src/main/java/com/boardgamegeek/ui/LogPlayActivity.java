package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuBuilder.Callback;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Chronometer;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ColorAssignmentCompleteEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.ColorAssignerTask;
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter;
import com.boardgamegeek.ui.dialog.NumberPadDialogFragment;
import com.boardgamegeek.ui.widget.DatePickerDialogFragment;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShowcaseViewWizard;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.amlcurran.showcaseview.targets.Target;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

public class LogPlayActivity extends AppCompatActivity implements OnDateSetListener {
	private static final int HELP_VERSION = 3;
	private static final int REQUEST_ADD_PLAYER = 999;

	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_END_PLAY = "END_PLAY";
	public static final String KEY_REMATCH = "REMATCH";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	public static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	private static final String KEY_QUANTITY_SHOWN = "QUANTITY_SHOWN";
	private static final String KEY_LENGTH_SHOWN = "LENGTH_SHOWN";
	private static final String KEY_LOCATION_SHOWN = "LOCATION_SHOWN";
	private static final String KEY_INCOMPLETE_SHOWN = "INCOMPLETE_SHOWN";
	private static final String KEY_NO_WIN_STATS_SHOWN = "NO_WIN_STATS_SHOWN";
	private static final String KEY_COMMENTS_SHOWN = "COMMENTS_SHOWN";
	private static final String KEY_PLAYERS_SHOWN = "PLAYERS_SHOWN";
	private static final String KEY_DELETE_ON_CANCEL = "DELETE_ON_CANCEL";
	private static final String DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG";
	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYERS = 1 << 1;
	private static final int TOKEN_ID = 1 << 2;
	private static final int TOKEN_UNINITIALIZED = 1 << 31;
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
	private boolean mRematch;
	private String mThumbnailUrl;
	private String mImageUrl;

	private QueryHandler mHandler;
	private int mOutstandingQueries = TOKEN_UNINITIALIZED;

	private Play mPlay;
	private Play mOriginalPlay;
	private final Random mRandom = new Random();
	private PlayAdapter mPlayAdapter;
	private AutoCompleteAdapter locationAdapter;
	private AlertDialog.Builder mAddPlayersBuilder;
	private final List<Player> mPlayersToAdd = new ArrayList<>();
	private final List<String> userNames = new ArrayList<>();
	private final List<String> names = new ArrayList<>();

	@BindView(R.id.header) TextView mHeaderView;
	@BindView(R.id.log_play_date) TextView mDateButton;
	@BindView(R.id.log_play_quantity) EditText mQuantityView;
	@BindView(R.id.log_play_length) EditText mLengthView;
	@BindView(R.id.log_play_location) AutoCompleteTextView mLocationView;
	@BindView(R.id.log_play_incomplete) SwitchCompat mIncompleteView;
	@BindView(R.id.log_play_no_win_stats) SwitchCompat mNoWinStatsView;
	@BindView(R.id.timer) Chronometer mTimer;
	@BindView(R.id.timer_toggle) ImageView mTimerToggle;
	@BindView(R.id.log_play_comments) EditText mCommentsView;
	@BindView(R.id.log_play_players_header) LinearLayout mPlayerHeader;
	@BindView(R.id.log_play_players_label) TextView mPlayerLabel;
	@BindView(R.id.fab) FloatingActionButton mFab;
	private DragSortListView mPlayerList;
	private DatePickerDialogFragment mDatePickerFragment;
	private MenuBuilder mFullPopupMenu;
	private MenuBuilder mShortPopupMenu;
	private ShowcaseViewWizard showcaseWizard;
	@ColorInt private int mFabColor;

	@State boolean isUserShowingLocation;
	@State boolean isUserShowingLength;
	@State boolean isUserShowingQuantity;
	@State boolean isUserShowingIncomplete;
	@State boolean isUserShowingNoWinStats;
	@State boolean isUserShowingComments;
	@State boolean isUserShowingPlayers;
	@State boolean shouldDeletePlayOnActivityCancel;
	@State boolean arePlayersCustomSorted;

	private boolean isLaunchingActivity;
	private boolean shouldSaveOnPause = true;

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

		@DebugLog
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
					if (mPlay.getPlayerCount() > 0) {
						arePlayersCustomSorted = mPlay.arePlayersCustomSorted();
					} else {
						arePlayersCustomSorted = getIntent().getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false);
					}
					if ((mOutstandingQueries & TOKEN_ID) != 0) {
						mHandler.startQuery(TOKEN_ID, null, Plays.CONTENT_SIMPLE_URI, ID_PROJECTION, null, null, null);
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

	@DebugLog
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
				if (mRematch) {
					mPlay.playId = mPlayId;
					mPlay = PlayBuilder.rematch(mPlay);
				}
				mOriginalPlay = PlayBuilder.copy(mPlay);
				finishDataLoad();
			}
		}
	}

	@DebugLog
	private void finishDataLoad() {
		mOutstandingQueries = 0;
		if (mEndPlay) {
			NotificationUtils.cancel(LogPlayActivity.this, NotificationUtils.ID_PLAY_TIMER);
		} else {
			maybeShowNotification();
		}

		bindUi();
		findViewById(R.id.progress).setVisibility(View.GONE);
		findViewById(R.id.form).setVisibility(View.VISIBLE);
	}

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplay);
		ToolbarUtils.setDoneCancelActionBarView(this, mActionBarListener);
		setUiVariables();
		mHandler = new QueryHandler(getContentResolver());

		final Intent intent = getIntent();
		mPlayId = intent.getIntExtra(KEY_PLAY_ID, BggContract.INVALID_ID);
		mGameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(KEY_GAME_NAME);
		mEndPlay = intent.getBooleanExtra(KEY_END_PLAY, false);
		mRematch = intent.getBooleanExtra(KEY_REMATCH, false);
		mThumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL);
		mImageUrl = intent.getStringExtra(KEY_IMAGE_URL);

		if (mGameId <= 0) {
			String message = "Can't log a play without a game ID.";
			Timber.w(message);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			finish();
		}
		mHeaderView.setText(mGameName);

		mFabColor = ContextCompat.getColor(this, R.color.accent);
		ImageUtils.safelyLoadImage((ImageView) findViewById(R.id.thumbnail), mImageUrl, new ImageUtils.Callback() {
			@Override
			public void onSuccessfulImageLoad(Palette palette) {
				mHeaderView.setBackgroundResource(R.color.black_overlay_light);
				mFabColor = PaletteUtils.getIconSwatch(palette).getRgb();
				mFab.setBackgroundTintList(ColorStateList.valueOf(mFabColor));
				mFab.show();
			}

			@Override
			public void onFailedImageLoad() {
				mFab.show();
			}
		});

		Icepick.restoreInstanceState(this, savedInstanceState);
		if (savedInstanceState != null) {
			mPlay = PlayBuilder.fromBundle(savedInstanceState, "P");
			mOriginalPlay = PlayBuilder.fromBundle(savedInstanceState, "O");
		}
		startQuery();

		setUpShowcaseViewWizard();
		showcaseWizard.maybeShowHelp();
	}

	@DebugLog
	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@DebugLog
	@Override
	protected void onResume() {
		super.onResume();
		isLaunchingActivity = false;
		setViewVisibility();

		locationAdapter = new AutoCompleteAdapter(this, Plays.LOCATION, Plays.buildLocationsUri());
		mLocationView.setAdapter(locationAdapter);
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mOutstandingQueries == 0) {
			captureForm();
			PlayBuilder.toBundle(mPlay, outState, "P");
			PlayBuilder.toBundle(mOriginalPlay, outState, "O");
		}
		Icepick.saveInstanceState(this, outState);
	}

	@DebugLog
	@Override
	protected void onPause() {
		super.onPause();
		locationAdapter.changeCursor(null);
		if (shouldSaveOnPause && !isLaunchingActivity) {
			saveDraft(false);
		}
	}

	@DebugLog
	@Override
	protected void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@DebugLog
	@Override
	public void onBackPressed() {
		saveDraft(true);
		setResult(Activity.RESULT_OK);
		finish();
	}

	@DebugLog
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Player player = data.getParcelableExtra(LogPlayerActivity.KEY_PLAYER);
			if (requestCode == REQUEST_ADD_PLAYER) {
				mPlay.addPlayer(player);
				maybeShowNotification();
				// prompt for another player
				Intent intent = new Intent();
				editPlayer(intent, REQUEST_ADD_PLAYER);
			} else {
				mPlay.replacePlayer(player, requestCode);
			}
			bindUiPlayers();
		}
	}

	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ColorAssignmentCompleteEvent event) {
		EventBus.getDefault().removeStickyEvent(event);
		if (event.isSuccessful()) {
			bindUiPlayers();
		}
		if (event.getMessageId() != 0) {
			Snackbar.make(mPlayerList, event.getMessageId(), Snackbar.LENGTH_LONG).show();
		}
	}

	@DebugLog
	private void setUpShowcaseViewWizard() {
		showcaseWizard = new ShowcaseViewWizard(this, HelpUtils.HELP_LOGPLAY_KEY, HELP_VERSION);
		showcaseWizard.addTarget(R.string.help_logplay, Target.NONE);
	}

	@DebugLog
	private void setUiVariables() {
		mPlayerList = (DragSortListView) findViewById(android.R.id.list);
		mPlayerList.addHeaderView(View.inflate(this, R.layout.header_logplay, null), null, false);
		mPlayerList.addFooterView(View.inflate(this, R.layout.footer_fab_buffer, null), null, false);

		ButterKnife.bind(this);

		mPlayAdapter = new PlayAdapter();
		mPlayerList.setAdapter(mPlayAdapter);

		mDatePickerFragment = (DatePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(DATE_PICKER_DIALOG_TAG);
		if (mDatePickerFragment != null) {
			mDatePickerFragment.setOnDateSetListener(this);
		}

		mPlayerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int offsetPosition = position - 1; // offset by the list header
				Player player = (Player) mPlayAdapter.getItem(offsetPosition);
				Intent intent = new Intent();
				intent.putExtra(LogPlayerActivity.KEY_PLAYER, player);
				intent.putExtra(LogPlayerActivity.KEY_END_PLAY, mEndPlay);
				intent.putExtra(LogPlayerActivity.KEY_FAB_COLOR, mFabColor);
				if (!arePlayersCustomSorted) {
					intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, player.getSeat());
				}
				editPlayer(intent, offsetPosition);
			}
		});
	}

	@DebugLog
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

	@DebugLog
	private void bindLength() {
		mLengthView.setTextKeepState((mPlay.length == Play.LENGTH_DEFAULT) ? "" : String.valueOf(mPlay.length));
		UIUtils.startTimerWithSystemTime(mTimer, mPlay.startTime);
	}

	@DebugLog
	private void bindUiPlayers() {
		// calculate player count
		Resources r = getResources();
		int playerCount = mPlay.getPlayerCount();
		if (playerCount <= 0) {
			mPlayerLabel.setText(r.getString(R.string.title_players));
		} else {
			mPlayerLabel.setText(r.getString(R.string.title_players) + " - " + String.valueOf(playerCount));
		}

		mPlayerList.setDragEnabled(!arePlayersCustomSorted);
		mPlayAdapter.notifyDataSetChanged();
		setViewVisibility();
		maybeShowNotification();
	}

	@DebugLog
	private void setViewVisibility() {
		if (mPlay == null) {
			// all fields should be hidden, so it shouldn't matter
			return;
		}

		hideRow(shouldHideLength() && !mPlay.hasStarted(), findViewById(R.id.log_play_length_root));
		if (mPlay.hasStarted()) {
			mLengthView.setVisibility(View.GONE);
			mTimer.setVisibility(View.VISIBLE);
		} else {
			mLengthView.setVisibility(View.VISIBLE);
			mTimer.setVisibility(View.GONE);
		}
		if (mPlay.hasStarted()) {
			mTimerToggle.setEnabled(true);
			mTimerToggle.setImageResource(R.drawable.ic_timer_off);
		} else if (DateUtils.isToday(mPlay.getDateInMillis() + mPlay.length * 60 * 1000)) {
			mTimerToggle.setEnabled(true);
			mTimerToggle.setImageResource(R.drawable.ic_timer);
		} else {
			mTimerToggle.setEnabled(false);
		}

		hideRow(shouldHideQuantity(), findViewById(R.id.log_play_quantity_root));
		hideRow(shouldHideLocation(), findViewById(R.id.log_play_location_root));
		hideRow(shouldHideIncomplete(), mIncompleteView);
		hideRow(shouldHideNoWinStats(), mNoWinStatsView);
		hideRow(shouldHideComments(), findViewById(R.id.log_play_comments_root));
		hideRow(shouldHidePlayers(), mPlayerHeader);
		findViewById(R.id.clear_players).setEnabled(mPlay.getPlayerCount() > 0);
	}

	@DebugLog
	private boolean hideRow(boolean shouldHide, View view) {
		if (shouldHide) {
			view.setVisibility(View.GONE);
			return true;
		}
		view.setVisibility(View.VISIBLE);
		return false;
	}

	@DebugLog
	private boolean shouldHideLocation() {
		return !PreferencesUtils.showLogPlayLocation(this) && !isUserShowingLocation && TextUtils.isEmpty(mPlay.location);
	}

	@DebugLog
	private boolean shouldHideLength() {
		return !PreferencesUtils.showLogPlayLength(this) && !isUserShowingLength && !(mPlay.length > 0);
	}

	@DebugLog
	private boolean shouldHideQuantity() {
		return !PreferencesUtils.showLogPlayQuantity(this) && !isUserShowingQuantity && !(mPlay.quantity > 1);
	}

	@DebugLog
	private boolean shouldHideIncomplete() {
		return !PreferencesUtils.showLogPlayIncomplete(this) && !isUserShowingIncomplete && !mPlay.Incomplete();
	}

	@DebugLog
	private boolean shouldHideNoWinStats() {
		return !PreferencesUtils.showLogPlayNoWinStats(this) && !isUserShowingNoWinStats && !mPlay.NoWinStats();
	}

	@DebugLog
	private boolean shouldHideComments() {
		return !PreferencesUtils.showLogPlayComments(this) && !isUserShowingComments && TextUtils.isEmpty(mPlay.comments);
	}

	@DebugLog
	private boolean shouldHidePlayers() {
		return !PreferencesUtils.showLogPlayPlayerList(this) && !isUserShowingPlayers && (mPlay.getPlayerCount() == 0);
	}

	@DebugLog
	private void startQuery() {
		if (mPlay != null) {
			// we already have the play from the saved instance
			finishDataLoad();
		} else {
			if (mPlayId > 0) {
				// Editing or copying an existing play, so retrieve it
				shouldDeletePlayOnActivityCancel = false;
				mOutstandingQueries = TOKEN_PLAY | TOKEN_PLAYERS;
				if (mRematch) {
					shouldDeletePlayOnActivityCancel = true;
					mOutstandingQueries |= TOKEN_ID;
				}
				mHandler.startQuery(TOKEN_PLAY, null, Plays.buildPlayUri(mPlayId), PLAY_PROJECTION, null, null, null);
			} else {
				// Starting a new play
				shouldDeletePlayOnActivityCancel = true;
				arePlayersCustomSorted = getIntent().getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false);
				mOutstandingQueries = TOKEN_ID;
				mHandler.startQuery(TOKEN_ID, null, Plays.CONTENT_SIMPLE_URI, ID_PROJECTION, null, null, null);
			}
		}
	}

	@DebugLog
	private boolean onActionBarItemSelected(int itemId) {
		if (mOutstandingQueries == 0) {
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
		}
		return false;
	}

	@DebugLog
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

	@DebugLog
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
		shouldSaveOnPause = false;
		if (syncStatus != Play.SYNC_STATUS_PENDING_DELETE) {
			captureForm();
		}
		mPlay.syncStatus = syncStatus;
		new PlayPersister(this).save(mPlay);
		return true;
	}

	@DebugLog
	private void cancel() {
		shouldSaveOnPause = false;
		captureForm();
		if (mPlay == null || mPlay.equals(mOriginalPlay)) {
			if (shouldDeletePlayOnActivityCancel) {
				if (save(Play.SYNC_STATUS_PENDING_DELETE)) {
					triggerUpload();
				}
			}
			setResult(RESULT_CANCELED);
			finish();
		} else {
			if (shouldDeletePlayOnActivityCancel) {
				DialogUtils.createConfirmationDialog(this, R.string.are_you_sure_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (save(Play.SYNC_STATUS_PENDING_DELETE)) {
								triggerUpload();
								NotificationUtils.cancel(LogPlayActivity.this, NotificationUtils.ID_PLAY_TIMER);
							}
							setResult(RESULT_CANCELED);
							finish();
						}
					}).show();
			} else {
				DialogUtils.createCancelDialog(this).show();
			}
		}
	}

	@DebugLog
	private void triggerUpload() {
		SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	@DebugLog
	@OnClick(R.id.fab)
	public void addField() {
		final CharSequence[] array = createAddFieldArray();
		if (array == null || array.length == 0) {
			return;
		}
		new Builder(this).setTitle(R.string.add_field)
			.setItems(array, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					View viewToFocus = null;
					View viewToScroll = null;
					Resources r = getResources();
					String selection = array[which].toString();

					if (selection.equals(r.getString(R.string.location))) {
						isUserShowingLocation = true;
						viewToFocus = mLocationView;
						viewToScroll = findViewById(R.id.log_play_location_root);
					} else if (selection.equals(r.getString(R.string.length))) {
						isUserShowingLength = true;
						viewToFocus = mLengthView;
						viewToScroll = findViewById(R.id.log_play_length_root);
					} else if (selection.equals(r.getString(R.string.quantity))) {
						isUserShowingQuantity = true;
						viewToFocus = mQuantityView;
						viewToScroll = findViewById(R.id.log_play_quantity_root);
					} else if (selection.equals(r.getString(R.string.incomplete))) {
						isUserShowingIncomplete = true;
						mIncompleteView.setChecked(true);
						viewToScroll = mIncompleteView;
					} else if (selection.equals(r.getString(R.string.noWinStats))) {
						isUserShowingNoWinStats = true;
						mNoWinStatsView.setChecked(true);
						viewToScroll = mNoWinStatsView;
					} else if (selection.equals(r.getString(R.string.comments))) {
						isUserShowingComments = true;
						viewToFocus = mCommentsView;
						viewToScroll = mCommentsView;
					} else if (selection.equals(r.getString(R.string.title_colors))) {
						if (mPlay.hasColors()) {
							AlertDialog.Builder builder = new AlertDialog.Builder(LogPlayActivity.this)
								.setTitle(R.string.title_clear_colors)
								.setMessage(R.string.msg_clear_colors)
								.setCancelable(true)
								.setNegativeButton(R.string.keep, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, mPlay));
									}
								})
								.setPositiveButton(R.string.clear, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										for (Player player : mPlay.getPlayers()) {
											player.color = "";
										}
										TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, mPlay));
									}
								});
							builder.show();
						} else {
							TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, mPlay));
						}
					} else if (selection.equals(r.getString(R.string.title_players))) {
						if (shouldHidePlayers()) {
							isUserShowingPlayers = true;
							viewToScroll = mPlayerHeader;
						}
						addPlayers();
					} else if (selection.equals(r.getString(R.string.title_player))) {
						if (shouldHidePlayers()) {
							isUserShowingPlayers = true;
							viewToScroll = mPlayerHeader;
						}
						addPlayer();
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

	@DebugLog
	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<>();
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
		if (mPlay.getPlayerCount() > 0) {
			list.add(r.getString(R.string.title_colors));
		}
		list.add(r.getString(R.string.title_players));
		list.add(r.getString(R.string.title_player));

		CharSequence[] array = {};
		array = list.toArray(array);
		return array;
	}

	@DebugLog
	private void addPlayers() {
		if (!showPlayersToAddDialog()) {
			editPlayer(new Intent(), REQUEST_ADD_PLAYER);
		}
	}

	@DebugLog
	private void addPlayer() {
		if (PreferencesUtils.editPlayer(this)) {
			editPlayer(new Intent(), REQUEST_ADD_PLAYER);
		} else {
			Player player = new Player();
			if (!arePlayersCustomSorted) {
				player.setSeat(mPlay.getPlayerCount() + 1);
			}
			mPlay.addPlayer(player);
			bindUiPlayers();
			mPlayerList.smoothScrollToPosition(mPlayerList.getCount());
		}
	}

	@DebugLog
	private boolean containsPlayer(String username, String name) {
		for (Player p : mPlay.getPlayers()) {
			if (!TextUtils.isEmpty(username)) {
				if (username.equals(p.username)) {
					return true;
				}
			} else if (!TextUtils.isEmpty(name)) {
				if (TextUtils.isEmpty(p.username) && name.equals(p.name)) {
					return true;
				}
			}
		}
		return false;
	}

	@DebugLog
	private boolean showPlayersToAddDialog() {
		if (mAddPlayersBuilder == null) {
			mAddPlayersBuilder = new AlertDialog.Builder(this).setTitle(R.string.title_add_players)
				.setPositiveButton(android.R.string.ok, addPlayersButtonClickListener())
				.setNeutralButton(R.string.more, addPlayersButtonClickListener())
				.setNegativeButton(android.R.string.cancel, null);
		}

		captureForm(); // to get location

		mPlayersToAdd.clear();
		userNames.clear();
		names.clear();
		List<String> descriptions = new ArrayList<>();

		String selection = null;
		String[] selectionArgs = null;
		if (!TextUtils.isEmpty(mPlay.location)) {
			selection = Plays.LOCATION + "=?";
			selectionArgs = new String[] { mPlay.location };
		}

		Cursor cursor = null;
		try {
			cursor = getContentResolver().query(Plays.buildPlayersByUniqueNameUri(),
				new String[] { PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.DESCRIPTION,
					PlayPlayers.COUNT, PlayPlayers.UNIQUE_NAME }, selection, selectionArgs, PlayPlayers.SORT_BY_COUNT);
			while (cursor != null && cursor.moveToNext()) {
				String username = cursor.getString(1);
				String name = cursor.getString(2);
				if (!containsPlayer(username, name)) {
					userNames.add(username);
					names.add(name);
					descriptions.add(cursor.getString(3));
				}
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
					player.username = userNames.get(which);
					player.name = names.get(which);
					if (isChecked) {
						mPlayersToAdd.add(player);
					} else {
						mPlayersToAdd.remove(player);
					}
				}
			}).create().show();

		return true;
	}

	@DebugLog
	private DialogInterface.OnClickListener addPlayersButtonClickListener() {
		return new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mPlay.setPlayers(mPlayersToAdd);
				if (!arePlayersCustomSorted) {
					mPlay.pickStartPlayer(0);
				}
				bindUiPlayers();
				if (which == DialogInterface.BUTTON_NEUTRAL) {
					editPlayer(new Intent(), REQUEST_ADD_PLAYER);
				}
			}
		};
	}

	@DebugLog
	@OnClick(R.id.log_play_date)
	public void onDateClick() {
		if (mDatePickerFragment == null) {
			mDatePickerFragment = new DatePickerDialogFragment();
			mDatePickerFragment.setOnDateSetListener(this);
		}
		final FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.executePendingTransactions();
		mDatePickerFragment.setCurrentDateInMillis(mPlay.getDateInMillis());
		mDatePickerFragment.show(fragmentManager, DATE_PICKER_DIALOG_TAG);
	}

	@DebugLog
	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		if (mPlay != null) {
			mPlay.setDate(year, monthOfYear, dayOfMonth);
			setDateButtonText();
			setViewVisibility();
		}
	}

	@DebugLog
	private void setDateButtonText() {
		mDateButton.setText(mPlay.getDateForDisplay(this));
	}

	@DebugLog
	@OnClick(R.id.timer_toggle)
	public void onTimer() {
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
				DialogUtils.createConfirmationDialog(this, R.string.are_you_sure_timer_reset,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							startTimer();
						}
					}).show();
			}
		}
	}

	@DebugLog
	private void startTimer() {
		mPlay.start();
		bindLength();
		setViewVisibility();
		maybeShowNotification();
	}

	@DebugLog
	@OnClick(R.id.player_sort)
	public void onPlayerSort(View view) {
		MenuPopupHelper popup;
		if (!arePlayersCustomSorted && mPlay.getPlayerCount() > 1) {
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
			mFullPopupMenu.getItem(0).setChecked(arePlayersCustomSorted);
			popup = new MenuPopupHelper(this, mFullPopupMenu, view);
		} else {
			if (mShortPopupMenu == null) {
				mShortPopupMenu = new MenuBuilder(this);
				MenuItem mi = mShortPopupMenu.add(MenuBuilder.NONE, R.id.menu_custom_player_order, MenuBuilder.NONE,
					R.string.menu_custom_player_order);
				mi.setCheckable(true);
				mShortPopupMenu.setCallback(popupMenuCallback());
			}
			mShortPopupMenu.getItem(0).setChecked(arePlayersCustomSorted);
			popup = new MenuPopupHelper(this, mShortPopupMenu, view);
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
						if (arePlayersCustomSorted) {
							if (mPlay.hasStartingPositions() && mPlay.arePlayersCustomSorted()) {
								Dialog dialog = DialogUtils.createConfirmationDialog(LogPlayActivity.this,
									R.string.are_you_sure_player_sort_custom_off,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											mPlay.pickStartPlayer(0);
											arePlayersCustomSorted = !arePlayersCustomSorted;
											bindUiPlayers();
										}
									});
								dialog.show();
							} else {
								mPlay.pickStartPlayer(0);
								arePlayersCustomSorted = !arePlayersCustomSorted;
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
											arePlayersCustomSorted = !arePlayersCustomSorted;
											bindUiPlayers();
										}
									}).setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											arePlayersCustomSorted = !arePlayersCustomSorted;
											mPlay.clearPlayerPositions();
											bindUiPlayers();
										}
									});
								builder = DialogUtils.addAlertIcon(builder);
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

	@DebugLog
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

	@DebugLog
	private CharSequence[] createArrayOfPlayerDescriptions() {
		String playerPrefix = getResources().getString(R.string.generic_player);
		List<CharSequence> list = new ArrayList<>();
		for (int i = 0; i < mPlay.getPlayerCount(); i++) {
			Player p = mPlay.getPlayers().get(i);
			String name = p.getDescription();
			if (TextUtils.isEmpty(name)) {
				name = String.format(playerPrefix, (i + 1));
			}
			list.add(name);
		}
		CharSequence[] array = {};
		array = list.toArray(array);
		return array;
	}

	@DebugLog
	private void notifyStartPlayer() {
		Player p = mPlay.getPlayerAtSeat(1);
		if (p != null) {
			String name = p.getDescription();
			if (TextUtils.isEmpty(name)) {
				name = String.format(getResources().getString(R.string.generic_player), 1);
			}
			Toast.makeText(this, String.format(getResources().getString(R.string.notification_start_player), name),
				Toast.LENGTH_SHORT).show();
		}
	}

	@DebugLog
	@OnClick(R.id.clear_players)
	public void onClearPlayers() {
		DialogUtils.createConfirmationDialog(this, R.string.are_you_sure_players_clear,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mPlay.clearPlayers();
					bindUiPlayers();
				}
			}).show();
	}

	@DebugLog
	private void editPlayer(Intent intent, int requestCode) {
		isLaunchingActivity = true;
		intent.setClass(LogPlayActivity.this, LogPlayerActivity.class);
		intent.putExtra(LogPlayerActivity.KEY_GAME_ID, mPlay.gameId);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, mPlay.gameName);
		intent.putExtra(LogPlayerActivity.KEY_IMAGE_URL, mImageUrl);
		intent.putExtra(LogPlayerActivity.KEY_END_PLAY, mEndPlay);
		if (!arePlayersCustomSorted && requestCode == REQUEST_ADD_PLAYER) {
			intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, mPlay.getPlayerCount() + 1);
		}
		List<String> colors = new ArrayList<>();
		for (Player player : mPlay.getPlayers()) {
			colors.add(player.color);
		}
		intent.putExtra(LogPlayerActivity.KEY_USED_COLORS, colors.toArray(new String[colors.size()]));
		startActivityForResult(intent, requestCode);
	}

	/**
	 * Captures the data in the form in the mPlay object
	 */
	@DebugLog
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
		maybeShowNotification();
	}

	@DebugLog
	private void maybeShowNotification() {
		if (mPlay != null && mPlay.hasStarted()) {
			NotificationUtils.launchPlayingNotification(this, mPlay, mThumbnailUrl, mImageUrl);
		}
	}

	private class PlayAdapter extends BaseAdapter implements DropListener {
		@DebugLog
		@Override
		public int getCount() {
			return mPlay == null ? 0 : mPlay.getPlayerCount();
		}

		@DebugLog
		@Override
		public Object getItem(int position) {
			return (mPlay == null || mPlay.getPlayerCount() < position) ?
				null :
				mPlay.getPlayers().get(position);
		}

		@DebugLog
		@Override
		public long getItemId(int position) {
			return position;
		}

		@DebugLog
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new PlayerRow(LogPlayActivity.this);
				// HACK workaround to make row take up full width
				convertView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			}
			PlayerRow row = (PlayerRow) convertView;
			row.setAutoSort(!arePlayersCustomSorted);
			row.setPlayer((Player) getItem(position));
			row.setOnDeleteListener(new PlayerDeleteClickListener(position));
			row.setOnScoreListener(new PlayerScoreClickListener(position));
			return convertView;
		}

		@DebugLog
		@Override
		public void drop(int from, int to) {
			if (!mPlay.reorderPlayers(from + 1, to + 1)) {
				Toast.makeText(LogPlayActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
			}
			notifyDataSetChanged();
		}
	}

	private class PlayerDeleteClickListener implements View.OnClickListener {
		private final int mPosition;

		@DebugLog
		public PlayerDeleteClickListener(int position) {
			mPosition = position;
		}

		@DebugLog
		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(LogPlayActivity.this);
			builder
				.setTitle(R.string.are_you_sure_title)
				.setMessage(R.string.are_you_sure_delete_player)
				.setCancelable(false)
				.setNegativeButton(R.string.no, null)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Player player = (Player) mPlayAdapter.getItem(mPosition);
						Toast.makeText(LogPlayActivity.this, R.string.msg_player_deleted, Toast.LENGTH_SHORT).show();
						mPlay.removePlayer(player, !arePlayersCustomSorted);
						bindUiPlayers();
					}
				});
			builder.create().show();
		}
	}

	private class PlayerScoreClickListener implements View.OnClickListener {
		private final int mPosition;

		@DebugLog
		public PlayerScoreClickListener(int position) {
			mPosition = position;
		}

		@DebugLog
		@Override
		public void onClick(View v) {
			final Player player = mPlay.getPlayers().get(mPosition);
			final NumberPadDialogFragment fragment = NumberPadDialogFragment.newInstance(player.getDescription(), player.score, player.color);
			fragment.setOnDoneClickListener(new NumberPadDialogFragment.OnClickListener() {
				@Override
				public void onDoneClick(String output) {
					player.score = output;
					double highScore = mPlay.getHighScore();
					for (Player p : mPlay.getPlayers()) {
						double score = StringUtils.parseDouble(p.score, Double.MIN_VALUE);
						p.Win(score == highScore);
					}
					bindUiPlayers();
				}
			});
			DialogUtils.showFragment(LogPlayActivity.this, fragment, "score_dialog");
		}
	}
}
