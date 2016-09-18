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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
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
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.ColorAssignerTask;
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter;
import com.boardgamegeek.ui.dialog.NumberPadDialogFragment;
import com.boardgamegeek.ui.widget.DatePickerDialogFragment;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.ActivityUtils;
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
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

public class LogPlayActivity extends AppCompatActivity implements OnDateSetListener {
	private static final int HELP_VERSION = 3;
	private static final int REQUEST_ADD_PLAYER = 999;

	private static final String DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG";
	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYERS = 1 << 1;
	private static final int TOKEN_ID = 1 << 2;
	private static final int TOKEN_UNINITIALIZED = 1 << 31;
	private static final String[] ID_PROJECTION = { "MAX(plays." + Plays.PLAY_ID + ")" };

	private int playId;
	private int gameId;
	private String gameName;
	private boolean isRequestingToEndPlay;
	private boolean isRequestingRematch;
	private String thumbnailUrl;
	private String imageUrl;

	private QueryHandler queryHandler;
	private int outstandingQueries = TOKEN_UNINITIALIZED;

	private Play play;
	private Play originalPlay;
	private PlayAdapter playAdapter;
	private AutoCompleteAdapter locationAdapter;
	private AlertDialog.Builder addPlayersBuilder;
	private final List<Player> playersToAdd = new ArrayList<>();
	private final List<String> userNames = new ArrayList<>();
	private final List<String> names = new ArrayList<>();

	@BindView(R.id.coordinator) CoordinatorLayout coordinatorLayout;
	@BindView(R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(R.id.header) TextView headerView;
	@BindView(R.id.thumbnail) ImageView thumbnailView;
	@BindView(R.id.log_play_date) TextView dateButton;
	@BindView(R.id.log_play_quantity_root) View quantityRootView;
	@BindView(R.id.log_play_quantity) EditText quantityView;
	@BindView(R.id.log_play_length_root) View lengthRootView;
	@BindView(R.id.log_play_length) EditText lengthView;
	@BindView(R.id.timer) Chronometer timerView;
	@BindView(R.id.timer_toggle) ImageView timerToggleView;
	@BindView(R.id.log_play_location_root) View locationRootView;
	@BindView(R.id.log_play_location) AutoCompleteTextView locationView;
	@BindView(R.id.log_play_incomplete) SwitchCompat incompleteView;
	@BindView(R.id.log_play_no_win_stats) SwitchCompat noWinStatsView;
	@BindView(R.id.log_play_comments_root) View commentsRootView;
	@BindView(R.id.log_play_comments) EditText commentsView;
	@BindView(R.id.log_play_players_header) LinearLayout playerHeaderView;
	@BindView(R.id.clear_players) View clearPlayersButton;
	@BindView(R.id.log_play_players_label) TextView playerLabelView;
	@BindView(R.id.fab) FloatingActionButton fab;
	private DragSortListView playerList;
	private DatePickerDialogFragment datePickerFragment;
	private MenuBuilder fullPopupMenu;
	private MenuBuilder shortPopupMenu;
	private ShowcaseViewWizard showcaseWizard;
	@ColorInt private int fabColor;

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
					play = PlayBuilder.fromCursor(cursor);
					cursor.close();
					if (isRequestingToEndPlay) {
						play.end();
					}
					if ((outstandingQueries & TOKEN_PLAYERS) != 0) {
						queryHandler.startQuery(TOKEN_PLAYERS, null, Plays.buildPlayerUri(playId), PlayBuilder.PLAYER_PROJECTION, null, null, null);
					}
					setModelIfDone(token);
					break;
				case TOKEN_PLAYERS:
					try {
						PlayBuilder.addPlayers(cursor, play);
					} finally {
						cursor.close();
					}
					if (play.getPlayerCount() > 0) {
						arePlayersCustomSorted = play.arePlayersCustomSorted();
					} else {
						arePlayersCustomSorted = getIntent().getBooleanExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, false);
					}
					if ((outstandingQueries & TOKEN_ID) != 0) {
						queryHandler.startQuery(TOKEN_ID, null, Plays.CONTENT_SIMPLE_URI, ID_PROJECTION, null, null, null);
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
					playId = id;
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
			outstandingQueries &= ~queryType;
			if (outstandingQueries == 0) {
				if (play == null) {
					play = new Play(playId, gameId, gameName);
					play.setCurrentDate();

					long lastPlay = PreferencesUtils.getLastPlayTime(this);
					if (DateTimeUtils.howManyHoursOld(lastPlay) < 3) {
						play.location = PreferencesUtils.getLastPlayLocation(this);
						play.setPlayers(PreferencesUtils.getLastPlayPlayers(this));
						play.pickStartPlayer(0);
					}
				}
				if (isRequestingRematch) {
					play.playId = playId;
					play = PlayBuilder.rematch(play);
				}
				originalPlay = PlayBuilder.copy(play);
				finishDataLoad();
			}
		}
	}

	@DebugLog
	private void finishDataLoad() {
		outstandingQueries = 0;
		if (isRequestingToEndPlay) {
			NotificationUtils.cancel(LogPlayActivity.this, NotificationUtils.ID_PLAY_TIMER);
		} else {
			maybeShowNotification();
		}

		bindUi();
		progressView.hide();
		playerList.setVisibility(View.VISIBLE);
	}

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplay);
		ToolbarUtils.setDoneCancelActionBarView(this, mActionBarListener);
		setUiVariables();
		queryHandler = new QueryHandler(getContentResolver());

		final Intent intent = getIntent();
		playId = intent.getIntExtra(ActivityUtils.KEY_PLAY_ID, BggContract.INVALID_ID);
		gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
		isRequestingToEndPlay = intent.getBooleanExtra(ActivityUtils.KEY_END_PLAY, false);
		isRequestingRematch = intent.getBooleanExtra(ActivityUtils.KEY_REMATCH, false);
		thumbnailUrl = intent.getStringExtra(ActivityUtils.KEY_THUMBNAIL_URL);
		imageUrl = intent.getStringExtra(ActivityUtils.KEY_IMAGE_URL);

		if (gameId <= 0) {
			String message = "Can't log a play without a game ID.";
			Timber.w(message);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			finish();
		}
		headerView.setText(gameName);

		fabColor = ContextCompat.getColor(this, R.color.accent);
		ImageUtils.safelyLoadImage(thumbnailView, imageUrl, new ImageUtils.Callback() {
			@Override
			public void onSuccessfulImageLoad(Palette palette) {
				headerView.setBackgroundResource(R.color.black_overlay_light);
				fabColor = PaletteUtils.getIconSwatch(palette).getRgb();
				fab.setBackgroundTintList(ColorStateList.valueOf(fabColor));
				fab.show();
			}

			@Override
			public void onFailedImageLoad() {
				fab.show();
			}
		});

		Icepick.restoreInstanceState(this, savedInstanceState);
		if (savedInstanceState != null) {
			play = PlayBuilder.fromBundle(savedInstanceState, "P");
			originalPlay = PlayBuilder.fromBundle(savedInstanceState, "O");
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
		locationView.setAdapter(locationAdapter);
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outstandingQueries == 0) {
			PlayBuilder.toBundle(play, outState, "P");
			PlayBuilder.toBundle(originalPlay, outState, "O");
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
				play.addPlayer(player);
				maybeShowNotification();
				// prompt for another player
				Intent intent = new Intent();
				editPlayer(intent, REQUEST_ADD_PLAYER);
			} else {
				play.replacePlayer(player, requestCode);
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
			Snackbar.make(coordinatorLayout, event.getMessageId(), Snackbar.LENGTH_LONG).show();
		}
	}

	@DebugLog
	private void setUpShowcaseViewWizard() {
		showcaseWizard = new ShowcaseViewWizard(this, HelpUtils.HELP_LOGPLAY_KEY, HELP_VERSION);
		showcaseWizard.addTarget(R.string.help_logplay, Target.NONE);
	}

	@DebugLog
	private void setUiVariables() {
		playerList = (DragSortListView) findViewById(android.R.id.list);
		playerList.addHeaderView(View.inflate(this, R.layout.header_logplay, null), null, false);
		playerList.addFooterView(View.inflate(this, R.layout.footer_fab_buffer, null), null, false);

		ButterKnife.bind(this);

		playAdapter = new PlayAdapter();
		playerList.setAdapter(playAdapter);

		datePickerFragment = (DatePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(DATE_PICKER_DIALOG_TAG);
		if (datePickerFragment != null) {
			datePickerFragment.setOnDateSetListener(this);
		}

		playerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int offsetPosition = position - 1; // offset by the list header
				Player player = (Player) playAdapter.getItem(offsetPosition);
				Intent intent = new Intent();
				intent.putExtra(LogPlayerActivity.KEY_PLAYER, player);
				intent.putExtra(LogPlayerActivity.KEY_END_PLAY, isRequestingToEndPlay);
				intent.putExtra(LogPlayerActivity.KEY_FAB_COLOR, fabColor);
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
		quantityView.setTextKeepState((play.quantity == Play.QUANTITY_DEFAULT) ? "" : String.valueOf(play.quantity));
		bindLength();
		locationView.setTextKeepState(play.location);
		incompleteView.setChecked(play.Incomplete());
		noWinStatsView.setChecked(play.NoWinStats());
		commentsView.setTextKeepState(play.comments);
		bindUiPlayers();
	}

	@DebugLog
	private void bindLength() {
		lengthView.setTextKeepState((play.length == Play.LENGTH_DEFAULT) ? "" : String.valueOf(play.length));
		UIUtils.startTimerWithSystemTime(timerView, play.startTime);
	}

	@DebugLog
	private void bindUiPlayers() {
		// calculate player count
		Resources r = getResources();
		int playerCount = play.getPlayerCount();
		if (playerCount <= 0) {
			playerLabelView.setText(r.getString(R.string.title_players));
		} else {
			playerLabelView.setText(r.getString(R.string.title_players) + " - " + String.valueOf(playerCount));
		}

		playerList.setDragEnabled(!arePlayersCustomSorted);
		playAdapter.notifyDataSetChanged();
		setViewVisibility();
		maybeShowNotification();
	}

	@DebugLog
	private void setViewVisibility() {
		if (play == null) {
			// all fields should be hidden, so it shouldn't matter
			return;
		}

		hideRow(shouldHideLength() && !play.hasStarted(), lengthRootView);
		if (play.hasStarted()) {
			lengthView.setVisibility(View.GONE);
			timerView.setVisibility(View.VISIBLE);
		} else {
			lengthView.setVisibility(View.VISIBLE);
			timerView.setVisibility(View.GONE);
		}
		if (play.hasStarted()) {
			timerToggleView.setEnabled(true);
			timerToggleView.setImageResource(R.drawable.ic_timer_off);
		} else if (DateUtils.isToday(play.getDateInMillis() + play.length * 60 * 1000)) {
			timerToggleView.setEnabled(true);
			timerToggleView.setImageResource(R.drawable.ic_timer);
		} else {
			timerToggleView.setEnabled(false);
		}

		hideRow(shouldHideQuantity(), quantityRootView);
		hideRow(shouldHideLocation(), locationRootView);
		hideRow(shouldHideIncomplete(), incompleteView);
		hideRow(shouldHideNoWinStats(), noWinStatsView);
		hideRow(shouldHideComments(), commentsRootView);
		hideRow(shouldHidePlayers(), playerHeaderView);
		clearPlayersButton.setEnabled(play.getPlayerCount() > 0);
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
		return !PreferencesUtils.showLogPlayLocation(this) && !isUserShowingLocation && TextUtils.isEmpty(play.location);
	}

	@DebugLog
	private boolean shouldHideLength() {
		return !PreferencesUtils.showLogPlayLength(this) && !isUserShowingLength && !(play.length > 0);
	}

	@DebugLog
	private boolean shouldHideQuantity() {
		return !PreferencesUtils.showLogPlayQuantity(this) && !isUserShowingQuantity && !(play.quantity > 1);
	}

	@DebugLog
	private boolean shouldHideIncomplete() {
		return !PreferencesUtils.showLogPlayIncomplete(this) && !isUserShowingIncomplete && !play.Incomplete();
	}

	@DebugLog
	private boolean shouldHideNoWinStats() {
		return !PreferencesUtils.showLogPlayNoWinStats(this) && !isUserShowingNoWinStats && !play.NoWinStats();
	}

	@DebugLog
	private boolean shouldHideComments() {
		return !PreferencesUtils.showLogPlayComments(this) && !isUserShowingComments && TextUtils.isEmpty(play.comments);
	}

	@DebugLog
	private boolean shouldHidePlayers() {
		return !PreferencesUtils.showLogPlayPlayerList(this) && !isUserShowingPlayers && (play.getPlayerCount() == 0);
	}

	@DebugLog
	private void startQuery() {
		if (play != null) {
			// we already have the play from the saved instance
			finishDataLoad();
		} else {
			if (playId > 0) {
				// Editing or copying an existing play, so retrieve it
				shouldDeletePlayOnActivityCancel = false;
				outstandingQueries = TOKEN_PLAY | TOKEN_PLAYERS;
				if (isRequestingRematch) {
					shouldDeletePlayOnActivityCancel = true;
					outstandingQueries |= TOKEN_ID;
				}
				queryHandler.startQuery(TOKEN_PLAY, null, Plays.buildPlayUri(playId), PlayBuilder.PLAY_PROJECTION, null, null, null);
			} else {
				// Starting a new play
				shouldDeletePlayOnActivityCancel = true;
				arePlayersCustomSorted = getIntent().getBooleanExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, false);
				outstandingQueries = TOKEN_ID;
				queryHandler.startQuery(TOKEN_ID, null, Plays.CONTENT_SIMPLE_URI, ID_PROJECTION, null, null, null);
			}
		}
	}

	@DebugLog
	private boolean onActionBarItemSelected(int itemId) {
		if (outstandingQueries == 0) {
			switch (itemId) {
				case R.id.menu_done:
					if (play == null) {
						cancel();
					} else {
						if (play.hasStarted()) {
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
			if (!play.hasBeenSynced()
				&& DateUtils.isToday(play.getDateInMillis() + Math.max(60, play.length) * 60 * 1000)) {
				PreferencesUtils.putLastPlayTime(this, System.currentTimeMillis());
				PreferencesUtils.putLastPlayLocation(this, play.location);
				PreferencesUtils.putLastPlayPlayers(this, play.getPlayers());
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
		if (play == null) {
			return false;
		}
		shouldSaveOnPause = false;
		play.syncStatus = syncStatus;
		new PlayPersister(this).save(play);
		return true;
	}

	@DebugLog
	private void cancel() {
		shouldSaveOnPause = false;
		if (play == null || play.equals(originalPlay)) {
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
						viewToFocus = locationView;
						viewToScroll = locationRootView;
					} else if (selection.equals(r.getString(R.string.length))) {
						isUserShowingLength = true;
						viewToFocus = lengthView;
						viewToScroll = lengthRootView;
					} else if (selection.equals(r.getString(R.string.quantity))) {
						isUserShowingQuantity = true;
						viewToFocus = quantityView;
						viewToScroll = quantityRootView;
					} else if (selection.equals(r.getString(R.string.incomplete))) {
						isUserShowingIncomplete = true;
						incompleteView.setChecked(true);
						viewToScroll = incompleteView;
					} else if (selection.equals(r.getString(R.string.noWinStats))) {
						isUserShowingNoWinStats = true;
						noWinStatsView.setChecked(true);
						viewToScroll = noWinStatsView;
					} else if (selection.equals(r.getString(R.string.comments))) {
						isUserShowingComments = true;
						viewToFocus = commentsView;
						viewToScroll = commentsView;
					} else if (selection.equals(r.getString(R.string.title_colors))) {
						if (play.hasColors()) {
							AlertDialog.Builder builder = new AlertDialog.Builder(LogPlayActivity.this)
								.setTitle(R.string.title_clear_colors)
								.setMessage(R.string.msg_clear_colors)
								.setCancelable(true)
								.setNegativeButton(R.string.keep, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, play));
									}
								})
								.setPositiveButton(R.string.clear, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										for (Player player : play.getPlayers()) {
											player.color = "";
										}
										TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, play));
									}
								});
							builder.show();
						} else {
							TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, play));
						}
					} else if (selection.equals(r.getString(R.string.title_players))) {
						if (shouldHidePlayers()) {
							isUserShowingPlayers = true;
							viewToScroll = playerHeaderView;
						}
						addPlayers();
					} else if (selection.equals(r.getString(R.string.title_player))) {
						if (shouldHidePlayers()) {
							isUserShowingPlayers = true;
							viewToScroll = playerHeaderView;
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
						playerList.post(new Runnable() {
							@Override
							public void run() {
								playerList.smoothScrollBy(finalView.getBottom(), android.R.integer.config_longAnimTime);
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
		if (play.getPlayerCount() > 0) {
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
				player.setSeat(play.getPlayerCount() + 1);
			}
			play.addPlayer(player);
			bindUiPlayers();
			playerList.smoothScrollToPosition(playerList.getCount());
		}
	}

	@DebugLog
	private boolean containsPlayer(String username, String name) {
		for (Player p : play.getPlayers()) {
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
		if (addPlayersBuilder == null) {
			addPlayersBuilder = new AlertDialog.Builder(this).setTitle(R.string.title_add_players)
				.setPositiveButton(android.R.string.ok, addPlayersButtonClickListener())
				.setNeutralButton(R.string.more, addPlayersButtonClickListener())
				.setNegativeButton(android.R.string.cancel, null);
		}

		playersToAdd.clear();
		userNames.clear();
		names.clear();
		List<String> descriptions = new ArrayList<>();

		String selection = null;
		String[] selectionArgs = null;
		if (!TextUtils.isEmpty(play.location)) {
			selection = Plays.LOCATION + "=?";
			selectionArgs = new String[] { play.location };
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
		addPlayersBuilder
			.setMultiChoiceItems(descriptions.toArray(array), null, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Player player = new Player();
					player.username = userNames.get(which);
					player.name = names.get(which);
					if (isChecked) {
						playersToAdd.add(player);
					} else {
						playersToAdd.remove(player);
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
				play.setPlayers(playersToAdd);
				if (!arePlayersCustomSorted) {
					play.pickStartPlayer(0);
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
		if (datePickerFragment == null) {
			datePickerFragment = new DatePickerDialogFragment();
			datePickerFragment.setOnDateSetListener(this);
		}
		final FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.executePendingTransactions();
		datePickerFragment.setCurrentDateInMillis(play.getDateInMillis());
		datePickerFragment.show(fragmentManager, DATE_PICKER_DIALOG_TAG);
	}

	@DebugLog
	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		if (play != null) {
			play.setDate(year, monthOfYear, dayOfMonth);
			setDateButtonText();
			setViewVisibility();
		}
	}

	@DebugLog
	private void setDateButtonText() {
		dateButton.setText(play.getDateForDisplay(this));
	}

	@DebugLog
	@OnClick(R.id.timer_toggle)
	public void onTimer() {
		if (play.hasStarted()) {
			isRequestingToEndPlay = true;
			play.end();
			bindLength();
			setViewVisibility();
			NotificationUtils.cancel(this, NotificationUtils.ID_PLAY_TIMER);
			if (play.length > 0) {
				lengthView.setSelection(0, lengthView.getText().length());
				lengthView.requestFocus();
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(lengthView,
					InputMethodManager.SHOW_IMPLICIT);
			}
		} else {
			if (play.length == 0) {
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
		play.start();
		bindLength();
		setViewVisibility();
		maybeShowNotification();
	}

	@DebugLog
	@OnClick(R.id.player_sort)
	public void onPlayerSort(View view) {
		MenuPopupHelper popup;
		if (!arePlayersCustomSorted && play.getPlayerCount() > 1) {
			if (fullPopupMenu == null) {
				fullPopupMenu = new MenuBuilder(this);
				MenuItem mi = fullPopupMenu.add(MenuBuilder.NONE, R.id.menu_custom_player_order, MenuBuilder.NONE, R.string.menu_custom_player_order);
				mi.setCheckable(true);
				fullPopupMenu.add(MenuBuilder.NONE, R.id.menu_pick_start_player, 2, R.string.menu_pick_start_player);
				fullPopupMenu.add(MenuBuilder.NONE, R.id.menu_random_start_player, 3, R.string.menu_random_start_player);
				fullPopupMenu.add(MenuBuilder.NONE, R.id.menu_random_player_order, 4, R.string.menu_random_player_order);
				fullPopupMenu.setCallback(popupMenuCallback());
			}
			fullPopupMenu.getItem(0).setChecked(arePlayersCustomSorted);
			popup = new MenuPopupHelper(this, fullPopupMenu, view);
		} else {
			if (shortPopupMenu == null) {
				shortPopupMenu = new MenuBuilder(this);
				MenuItem mi = shortPopupMenu.add(MenuBuilder.NONE, R.id.menu_custom_player_order, MenuBuilder.NONE, R.string.menu_custom_player_order);
				mi.setCheckable(true);
				shortPopupMenu.setCallback(popupMenuCallback());
			}
			shortPopupMenu.getItem(0).setChecked(arePlayersCustomSorted);
			popup = new MenuPopupHelper(this, shortPopupMenu, view);
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
							if (play.hasStartingPositions() && play.arePlayersCustomSorted()) {
								Dialog dialog = DialogUtils.createConfirmationDialog(LogPlayActivity.this,
									R.string.are_you_sure_player_sort_custom_off,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											play.pickStartPlayer(0);
											arePlayersCustomSorted = !arePlayersCustomSorted;
											bindUiPlayers();
										}
									});
								dialog.show();
							} else {
								play.pickStartPlayer(0);
								arePlayersCustomSorted = !arePlayersCustomSorted;
								bindUiPlayers();
							}
						} else {
							if (play.hasStartingPositions()) {
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
											play.clearPlayerPositions();
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
						int newSeat = new Random().nextInt(play.getPlayerCount());
						play.pickStartPlayer(newSeat);
						notifyStartPlayer();
						bindUiPlayers();
						return true;
					case R.id.menu_random_player_order:
						play.randomizePlayerOrder();
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
					play.pickStartPlayer(which);
					notifyStartPlayer();
					bindUiPlayers();
				}
			}).show();
	}

	@DebugLog
	private CharSequence[] createArrayOfPlayerDescriptions() {
		String playerPrefix = getResources().getString(R.string.generic_player);
		List<CharSequence> list = new ArrayList<>();
		for (int i = 0; i < play.getPlayerCount(); i++) {
			Player p = play.getPlayers().get(i);
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
		Player p = play.getPlayerAtSeat(1);
		if (p != null) {
			String name = p.getDescription();
			if (TextUtils.isEmpty(name)) {
				name = String.format(getResources().getString(R.string.generic_player), 1);
			}
			Snackbar.make(coordinatorLayout, String.format(getResources().getString(R.string.notification_start_player), name), Snackbar.LENGTH_LONG).show();
		}
	}

	@DebugLog
	@OnClick(R.id.clear_players)
	public void onClearPlayers() {
		DialogUtils.createConfirmationDialog(this, R.string.are_you_sure_players_clear,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					play.clearPlayers();
					bindUiPlayers();
				}
			}).show();
	}

	@DebugLog
	private void editPlayer(Intent intent, int requestCode) {
		isLaunchingActivity = true;
		intent.setClass(LogPlayActivity.this, LogPlayerActivity.class);
		intent.putExtra(LogPlayerActivity.KEY_GAME_ID, play.gameId);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, play.gameName);
		intent.putExtra(LogPlayerActivity.KEY_IMAGE_URL, imageUrl);
		intent.putExtra(LogPlayerActivity.KEY_END_PLAY, isRequestingToEndPlay);
		if (!arePlayersCustomSorted && requestCode == REQUEST_ADD_PLAYER) {
			intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, play.getPlayerCount() + 1);
		}
		List<String> colors = new ArrayList<>();
		for (Player player : play.getPlayers()) {
			colors.add(player.color);
		}
		intent.putExtra(LogPlayerActivity.KEY_USED_COLORS, colors.toArray(new String[colors.size()]));
		startActivityForResult(intent, requestCode);
	}

	@OnFocusChange(R.id.log_play_location)
	public void onLocationFocusChange(EditText v, boolean hasFocus) {
		if (!hasFocus) {
			play.location = v.getText().toString().trim();
		}
	}

	@OnFocusChange(R.id.log_play_length)
	public void onLengthFocusChange(EditText v, boolean hasFocus) {
		if (!hasFocus) {
			play.length = StringUtils.parseInt(v.getText().toString().trim());
		}
	}

	@OnFocusChange(R.id.log_play_quantity)
	public void onQuantityFocusChange(EditText v, boolean hasFocus) {
		if (!hasFocus) {
			play.quantity = StringUtils.parseInt(v.getText().toString().trim(), 1);
		}
	}

	@OnCheckedChanged(R.id.log_play_incomplete)
	public void onIncompleteCheckedChanged() {
		play.setIncomplete(incompleteView.isChecked());
	}

	@OnCheckedChanged(R.id.log_play_no_win_stats)
	public void onNoWinStatsCheckedChanged() {
		play.setNoWinStats(noWinStatsView.isChecked());
	}

	@OnFocusChange(R.id.log_play_comments)
	public void onCommentsFocusChange(EditText v, boolean hasFocus) {
		if (!hasFocus) {
			play.comments = v.getText().toString().trim();
		}
	}

	@DebugLog
	private void maybeShowNotification() {
		if (play != null && play.hasStarted()) {
			NotificationUtils.launchPlayingNotification(this, play, thumbnailUrl, imageUrl);
		}
	}

	private class PlayAdapter extends BaseAdapter implements DropListener {
		@DebugLog
		@Override
		public int getCount() {
			return play == null ? 0 : play.getPlayerCount();
		}

		@DebugLog
		@Override
		public Object getItem(int position) {
			return (play == null || play.getPlayerCount() < position) ?
				null :
				play.getPlayers().get(position);
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
			final int finalPosition = position;
			row.setOnDeleteListener(new View.OnClickListener() {
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
								Player player = (Player) playAdapter.getItem(finalPosition);
								Snackbar.make(coordinatorLayout, R.string.msg_player_deleted, Snackbar.LENGTH_LONG).show();
								play.removePlayer(player, !arePlayersCustomSorted);
								bindUiPlayers();
							}
						});
					builder.create().show();
				}
			});
			row.setOnScoreListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final Player player = play.getPlayers().get(finalPosition);
					final NumberPadDialogFragment fragment = NumberPadDialogFragment.newInstance(player.getDescription(), player.score, player.color);
					fragment.setOnDoneClickListener(new NumberPadDialogFragment.OnClickListener() {
						@Override
						public void onDoneClick(String output) {
							player.score = output;
							double highScore = play.getHighScore();
							for (Player p : play.getPlayers()) {
								double score = StringUtils.parseDouble(p.score, Double.MIN_VALUE);
								p.Win(score == highScore);
							}
							bindUiPlayers();
						}
					});
					DialogUtils.showFragment(LogPlayActivity.this, fragment, "score_dialog");
				}
			});
			return convertView;
		}

		@DebugLog
		@Override
		public void drop(int from, int to) {
			if (!play.reorderPlayers(from + 1, to + 1)) {
				Toast.makeText(LogPlayActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
			}
			notifyDataSetChanged();
		}
	}
}
