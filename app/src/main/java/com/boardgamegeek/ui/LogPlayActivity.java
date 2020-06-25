package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.events.ColorAssignmentCompleteEvent;
import com.boardgamegeek.extensions.FloatingActionButtonUtils;
import com.boardgamegeek.extensions.PreferenceUtils;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayLocations;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.repository.PlayRepository;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.ColorAssignerTask;
import com.boardgamegeek.ui.adapter.AutoCompleteAdapter;
import com.boardgamegeek.ui.dialog.ColorPickerWithListenerDialogFragment;
import com.boardgamegeek.ui.dialog.NumberPadDialogFragment;
import com.boardgamegeek.ui.dialog.PlayRatingNumberPadDialogFragment;
import com.boardgamegeek.ui.dialog.ScoreNumberPadDialogFragment;
import com.boardgamegeek.ui.widget.DatePickerDialogFragment;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.ShowcaseViewWizard;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.ToolbarUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.amlcurran.showcaseview.targets.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.analytics.FirebaseAnalytics.Param;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.FragmentManager;
import androidx.palette.graphics.Palette;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class LogPlayActivity extends AppCompatActivity implements
	ColorPickerWithListenerDialogFragment.Listener,
	ScoreNumberPadDialogFragment.Listener,
	PlayRatingNumberPadDialogFragment.Listener {
	private static final String KEY_ID = "ID";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL";
	private static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	private static final String KEY_END_PLAY = "END_PLAY";
	private static final String KEY_REMATCH = "REMATCH";
	private static final String KEY_CHANGE_GAME = "CHANGE_GAME";
	private static final String KEY_INTERNAL_ID = "INTERNAL_ID";
	private static final String KEY_IS_USER_SHOWING_LOCATION = "IS_USER_SHOWING_LOCATION";
	private static final String KEY_IS_USER_SHOWING_LENGTH = "IS_USER_SHOWING_LENGTH";
	private static final String KEY_IS_USER_SHOWING_QUANTITY = "IS_USER_SHOWING_QUANTITY";
	private static final String KEY_IS_USER_SHOWING_INCOMPLETE = "IS_USER_SHOWING_INCOMPLETE";
	private static final String KEY_IS_USER_SHOWING_NO_WIN_STATS = "IS_USER_SHOWING_NO_WIN_STATS";
	private static final String KEY_IS_USER_SHOWING_COMMENTS = "IS_USER_SHOWING_COMMENTS";
	private static final String KEY_IS_USER_SHOWING_PLAYERS = "IS_USER_SHOWING_PLAYERS";
	private static final String KEY_SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL = "SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL";
	private static final String KEY_ARE_PLAYERS_CUSTOM_SORTED = "ARE_PLAYERS_CUSTOM_SORTED";
	private static final int HELP_VERSION = 3;
	private static final int REQUEST_ADD_PLAYER = 1;
	private static final int REQUEST_EDIT_PLAYER = 2;

	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYERS = 1 << 1;
	private static final int TOKEN_COLORS = 1 << 2;
	private static final int TOKEN_UNINITIALIZED = 1 << 15;
	private static final DecimalFormat SCORE_FORMAT = new DecimalFormat("0.#########");

	private FirebaseAnalytics firebaseAnalytics;
	private long internalId = BggContract.INVALID_ID;
	private int gameId;
	private String gameName;
	private boolean isRequestingToEndPlay;
	private boolean isRequestingRematch;
	private boolean isChangingGame;
	private String thumbnailUrl;
	private String imageUrl;
	private String heroImageUrl;
	private long internalIdToDelete = BggContract.INVALID_ID;

	private QueryHandler queryHandler;
	private int outstandingQueries = TOKEN_UNINITIALIZED;

	private Play play;
	private Play originalPlay;
	private PlayAdapter playAdapter;
	private LocationAdapter locationAdapter;
	private AlertDialog.Builder addPlayersBuilder;
	private Player lastRemovedPlayer;
	private final List<Player> playersToAdd = new ArrayList<>();
	private final List<String> userNames = new ArrayList<>();
	private final List<String> names = new ArrayList<>();
	private final ArrayList<String> gameColors = new ArrayList<>();

	@BindView(R.id.coordinator) CoordinatorLayout coordinatorLayout;
	@BindView(R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(R.id.fab) FloatingActionButton fab;
	@BindView(android.R.id.list) RecyclerView recyclerView;
	private ShowcaseViewWizard showcaseWizard;
	@ColorInt private int fabColor;
	private final Paint swipePaint = new Paint();
	private Bitmap deleteIcon;
	private Bitmap editIcon;
	@BindDimen(R.dimen.material_margin_horizontal) float horizontalPadding;
	private ItemTouchHelper itemTouchHelper;

	private boolean isUserShowingLocation;
	private boolean isUserShowingLength;
	private boolean isUserShowingQuantity;
	private boolean isUserShowingIncomplete;
	private boolean isUserShowingNoWinStats;
	private boolean isUserShowingComments;
	private boolean isUserShowingPlayers;
	private boolean shouldDeletePlayOnActivityCancel;
	private boolean arePlayersCustomSorted;

	private boolean isLaunchingActivity;
	private boolean shouldSaveOnPause = true;

	private SharedPreferences prefs;

	public static void logPlay(Context context, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl, boolean customPlayerSort) {
		Intent intent = createIntent(context, BggContract.INVALID_ID, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort);
		context.startActivity(intent);
	}

	public static void editPlay(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl) {
		Bundle bundle = new Bundle();
		bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Play");
		bundle.putString("Action", "Edit");
		bundle.putString("GameName", gameName);
		FirebaseAnalytics.getInstance(context).logEvent("DataManipulation", bundle);
		Intent intent = createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, false);
		context.startActivity(intent);
	}

	public static void endPlay(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl) {
		Intent intent = createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, false);
		intent.putExtra(KEY_END_PLAY, true);
		context.startActivity(intent);
	}

	public static void rematch(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl) {
		Intent intent = createRematchIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl);
		context.startActivity(intent);
	}

	public static void changeGame(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl) {
		Intent intent = createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, false);
		intent.putExtra(KEY_CHANGE_GAME, true);
		context.startActivity(intent);
	}

	public static Intent createRematchIntent(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl) {
		Intent intent = createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, false);
		intent.putExtra(KEY_REMATCH, true);
		return intent;
	}

	private static Intent createIntent(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl, boolean customPlayerSort) {
		Intent intent = new Intent(context, LogPlayActivity.class);
		intent.putExtra(KEY_ID, internalId);
		intent.putExtra(KEY_GAME_ID, gameId);
		intent.putExtra(KEY_GAME_NAME, gameName);
		intent.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		intent.putExtra(KEY_HERO_IMAGE_URL, heroImageUrl);
		intent.putExtra(KEY_CUSTOM_PLAYER_SORT, customPlayerSort);
		return intent;
	}

	private final View.OnClickListener actionBarListener = v -> onActionBarItemSelected(v.getId());

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
						queryHandler.startQuery(TOKEN_PLAYERS, null, Plays.buildPlayerUri(internalId), PlayBuilder.PLAYER_PROJECTION, null, null, null);
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
						arePlayersCustomSorted = getIntent().getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false);
					}
					setModelIfDone(token);
					break;
				case TOKEN_COLORS:
					if (cursor.getCount() == 0) {
						cursor.close();
					} else {
						try {
							if (cursor.moveToFirst()) {
								gameColors.clear();
								do {
									gameColors.add(cursor.getString(0));
								} while (cursor.moveToNext());
							}
						} finally {
							cursor.close();
						}
					}
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
					// create a new play
					play = new Play(gameId, gameName);
					play.setCurrentDate();

					long lastPlay = PreferenceUtils.getLastPlayTime(prefs);
					if (DateTimeUtils.howManyHoursOld(lastPlay) < 12) {
						play.location = PreferenceUtils.getLastPlayLocation(prefs);
						play.setPlayers(PreferenceUtils.getLastPlayPlayers(prefs));
						play.pickStartPlayer(0);
					}
				}
				if (isRequestingRematch) {
					play = PlayBuilder.rematch(play);
					internalId = BggContract.INVALID_ID;
				} else if (isChangingGame) {
					play = PlayBuilder.copy(play);
					play.playId = BggContract.INVALID_ID;
					play.gameId = gameId;
					play.gameName = gameName;
					internalIdToDelete = internalId;
					internalId = BggContract.INVALID_ID;
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
			cancelNotification();
		} else {
			maybeShowNotification();
		}

		playAdapter.refresh();
		progressView.hide();
		recyclerView.setVisibility(View.VISIBLE);
	}

	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		firebaseAnalytics = FirebaseAnalytics.getInstance(this);

		setContentView(R.layout.activity_logplay);
		ToolbarUtils.setDoneCancelActionBarView(this, actionBarListener);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		ButterKnife.bind(this);

		playAdapter = new PlayAdapter(this);
		recyclerView.setAdapter(playAdapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setHasFixedSize(true);

		swipePaint.setColor(ContextCompat.getColor(this, R.color.medium_blue));
		deleteIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete_white);
		editIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_edit_white);
		itemTouchHelper = new ItemTouchHelper(
			new SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
				@Override
				public void onChildDraw(Canvas c, RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
					if (!(viewHolder instanceof PlayAdapter.PlayerViewHolder)) return;
					if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
						View itemView = viewHolder.itemView;

						// fade and slide item
						float width = (float) itemView.getWidth();
						float alpha = 1.0f - Math.abs(dX) / width;
						itemView.setAlpha(alpha);
						itemView.setTranslationX(dX);

						// show background with an icon
						Bitmap icon = editIcon;
						if (dX > 0) {
							icon = deleteIcon;
						}
						float verticalPadding = (itemView.getHeight() - icon.getHeight()) / 2f;
						RectF background;
						Rect iconSrc;
						RectF iconDst;

						if (dX > 0) {
							swipePaint.setColor(ContextCompat.getColor(LogPlayActivity.this, R.color.delete));
							background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
							iconSrc = new Rect(
								0,
								0,
								Math.min((int) (dX - itemView.getLeft() - horizontalPadding), icon.getWidth()),
								icon.getHeight());
							iconDst = new RectF(
								(float) itemView.getLeft() + horizontalPadding,
								(float) itemView.getTop() + verticalPadding,
								Math.min(itemView.getLeft() + horizontalPadding + icon.getWidth(), dX),
								(float) itemView.getBottom() - verticalPadding);
						} else {
							swipePaint.setColor(ContextCompat.getColor(LogPlayActivity.this, R.color.edit));
							background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
							iconSrc = new Rect(
								Math.max(icon.getWidth() + (int) horizontalPadding + (int) dX, 0),
								0,
								icon.getWidth(),
								icon.getHeight());
							iconDst = new RectF(
								Math.max((float) itemView.getRight() + dX, (float) itemView.getRight() - horizontalPadding - icon.getWidth()),
								(float) itemView.getTop() + verticalPadding,
								(float) itemView.getRight() - horizontalPadding,
								(float) itemView.getBottom() - verticalPadding);
						}
						c.drawRect(background, swipePaint);
						c.drawBitmap(icon, iconSrc, iconDst, swipePaint);
					}
					super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
				}

				@Override
				public void onSwiped(ViewHolder viewHolder, int swipeDir) {
					final int position = playAdapter.getPlayerPosition(viewHolder.getAdapterPosition());
					if (swipeDir == ItemTouchHelper.RIGHT) {
						lastRemovedPlayer = playAdapter.getPlayer(position);
						if (lastRemovedPlayer == null) return;

						String description = lastRemovedPlayer.getDescription();
						if (TextUtils.isEmpty(description)) {
							description = getString(R.string.title_player);
						}
						String message = getString(R.string.msg_player_deleted, description);
						Snackbar
							.make(coordinatorLayout, message, Snackbar.LENGTH_INDEFINITE)
							.setAction(R.string.undo, v -> {
								if (lastRemovedPlayer == null) return;
								play.addPlayer(lastRemovedPlayer);
								playAdapter.notifyPlayerAdded(position);
							})
							.show();
						play.removePlayer(lastRemovedPlayer, !arePlayersCustomSorted);
						playAdapter.notifyPlayerRemoved(position);
					} else {
						editPlayer(position);
					}
				}

				@Override
				public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
					if (play == null || playAdapter == null) return false;
					if (!(target instanceof PlayAdapter.PlayerViewHolder)) return false;

					final int fromPosition = viewHolder.getAdapterPosition();
					final int toPosition = target.getAdapterPosition();

					int from = playAdapter.getPlayerPosition(fromPosition);
					int to = playAdapter.getPlayerPosition(toPosition);

					if (!play.reorderPlayers(from + 1, to + 1)) {
						Toast.makeText(LogPlayActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
					} else {
						playAdapter.notifyItemMoved(fromPosition, toPosition);
						return true;
					}
					return false;
				}

				@Override
				public void clearView(RecyclerView recyclerView, ViewHolder viewHolder) {
					super.clearView(recyclerView, viewHolder);
					viewHolder.itemView.setBackgroundColor(0);
					playAdapter.notifyPlayersChanged();
				}

				@Override
				public void onSelectedChanged(ViewHolder viewHolder, int actionState) {
					// We only want the active item to change
					if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
						viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(LogPlayActivity.this, R.color.light_blue_transparent));
					}
					super.onSelectedChanged(viewHolder, actionState);
				}

				@Override
				public int getMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
					if (arePlayersCustomSorted)
						return makeMovementFlags(0, getSwipeDirs(recyclerView, viewHolder));
					return super.getMovementFlags(recyclerView, viewHolder);
				}

				@Override
				public boolean isLongPressDragEnabled() {
					return false;
				}
			});
		itemTouchHelper.attachToRecyclerView(recyclerView);

		queryHandler = new QueryHandler(getContentResolver());

		final Intent intent = getIntent();
		internalId = intent.getLongExtra(KEY_ID, BggContract.INVALID_ID);
		gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		isRequestingToEndPlay = intent.getBooleanExtra(KEY_END_PLAY, false);
		isRequestingRematch = intent.getBooleanExtra(KEY_REMATCH, false);
		isChangingGame = intent.getBooleanExtra(KEY_CHANGE_GAME, false);
		thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL);
		imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
		heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL);

		if (thumbnailUrl == null) thumbnailUrl = "";
		if (imageUrl == null) imageUrl = "";
		if (heroImageUrl == null) heroImageUrl = "";

		if (gameId <= 0) {
			String message = "Can't log a play without a game ID.";
			Timber.w(message);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			finish();
		}

		playAdapter.notifyLayoutChanged(R.layout.row_log_play_player_header);

		if (savedInstanceState != null) {
			play = PlayBuilder.fromBundle(savedInstanceState, "P");
			originalPlay = PlayBuilder.fromBundle(savedInstanceState, "O");
			internalId = savedInstanceState.getLong(KEY_INTERNAL_ID, BggContract.INVALID_ID);
			isUserShowingLocation = savedInstanceState.getBoolean(KEY_IS_USER_SHOWING_LOCATION);
			isUserShowingLength = savedInstanceState.getBoolean(KEY_IS_USER_SHOWING_LENGTH);
			isUserShowingQuantity = savedInstanceState.getBoolean(KEY_IS_USER_SHOWING_QUANTITY);
			isUserShowingIncomplete = savedInstanceState.getBoolean(KEY_IS_USER_SHOWING_INCOMPLETE);
			isUserShowingNoWinStats = savedInstanceState.getBoolean(KEY_IS_USER_SHOWING_NO_WIN_STATS);
			isUserShowingComments = savedInstanceState.getBoolean(KEY_IS_USER_SHOWING_COMMENTS);
			isUserShowingPlayers = savedInstanceState.getBoolean(KEY_IS_USER_SHOWING_PLAYERS);
			shouldDeletePlayOnActivityCancel = savedInstanceState.getBoolean(KEY_SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL);
			arePlayersCustomSorted = savedInstanceState.getBoolean(KEY_ARE_PLAYERS_CUSTOM_SORTED);
		}
		startQuery();

		setUpShowcaseViewWizard();
		showcaseWizard.maybeShowHelp();
		fab.postDelayed(() -> fab.show(), 2000);
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
		locationAdapter = new LocationAdapter(this);
		playAdapter.refresh();
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (outstandingQueries == 0) {
			PlayBuilder.toBundle(play, outState, "P");
			PlayBuilder.toBundle(originalPlay, outState, "O");
		}
		outState.putLong(KEY_INTERNAL_ID, internalId);
		outState.putBoolean(KEY_IS_USER_SHOWING_LOCATION, isUserShowingLocation);
		outState.putBoolean(KEY_IS_USER_SHOWING_LENGTH, isUserShowingLength);
		outState.putBoolean(KEY_IS_USER_SHOWING_QUANTITY, isUserShowingQuantity);
		outState.putBoolean(KEY_IS_USER_SHOWING_INCOMPLETE, isUserShowingIncomplete);
		outState.putBoolean(KEY_IS_USER_SHOWING_NO_WIN_STATS, isUserShowingNoWinStats);
		outState.putBoolean(KEY_IS_USER_SHOWING_COMMENTS, isUserShowingComments);
		outState.putBoolean(KEY_IS_USER_SHOWING_PLAYERS, isUserShowingPlayers);
		outState.putBoolean(KEY_SHOULD_DELETE_PLAY_ON_ACTIVITY_CANCEL, shouldDeletePlayOnActivityCancel);
		outState.putBoolean(KEY_ARE_PLAYERS_CUSTOM_SORTED, arePlayersCustomSorted);
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
			int position = data.getIntExtra(LogPlayerActivity.KEY_POSITION, LogPlayerActivity.INVALID_POSITION);
			switch (requestCode) {
				case REQUEST_ADD_PLAYER:
					play.addPlayer(player);
					maybeShowNotification();
					addNewPlayer();
					break;
				case REQUEST_EDIT_PLAYER:
					if (position == LogPlayerActivity.INVALID_POSITION) {
						Timber.w("Invalid player position after edit");
					} else {
						play.replaceOrAddPlayer(player, position);
						playAdapter.notifyPlayerChanged(position);
						recyclerView.smoothScrollToPosition(position);
					}
					break;
				default:
					Timber.w("Received invalid request code: %d", requestCode);
					break;
			}
		} else if (resultCode == RESULT_CANCELED) {
			playAdapter.notifyPlayersChanged();
			recyclerView.smoothScrollToPosition(playAdapter.getItemCount());
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(ColorAssignmentCompleteEvent event) {
		firebaseAnalytics.logEvent("LogPlayColorAssignment", null);
		EventBus.getDefault().removeStickyEvent(event);
		if (event.isSuccessful()) {
			playAdapter.notifyPlayersChanged();
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
	private boolean shouldHideLocation() {
		return play != null && !PreferenceUtils.showLogPlayLocation(prefs) && !isUserShowingLocation && TextUtils.isEmpty(play.location);
	}

	@DebugLog
	private boolean shouldHideLength() {
		return play != null && !PreferenceUtils.showLogPlayLength(prefs) && !isUserShowingLength && !(play.length > 0) && !play.hasStarted();
	}

	@DebugLog
	private boolean shouldHideQuantity() {
		return play != null && !PreferenceUtils.showLogPlayQuantity(prefs) && !isUserShowingQuantity && !(play.quantity > 1);
	}

	@DebugLog
	private boolean shouldHideIncomplete() {
		return play != null && !PreferenceUtils.showLogPlayIncomplete(prefs) && !isUserShowingIncomplete && !play.incomplete;
	}

	@DebugLog
	private boolean shouldHideNoWinStats() {
		return play != null && !PreferenceUtils.showLogPlayNoWinStats(prefs) && !isUserShowingNoWinStats && !play.noWinStats;
	}

	@DebugLog
	private boolean shouldHideComments() {
		return play != null && !PreferenceUtils.showLogPlayComments(prefs) && !isUserShowingComments && TextUtils.isEmpty(play.comments);
	}

	@DebugLog
	private boolean shouldHidePlayers() {
		return play != null && !PreferenceUtils.showLogPlayPlayerList(prefs) && !isUserShowingPlayers && (play.getPlayerCount() == 0);
	}

	@DebugLog
	private void startQuery() {
		if (play != null) {
			// we already have the play from the saved instance
			finishDataLoad();
		} else {
			outstandingQueries = TOKEN_COLORS;
			if (internalId != BggContract.INVALID_ID) {
				// Editing or copying an existing play, so retrieve it
				shouldDeletePlayOnActivityCancel = false;
				outstandingQueries |= TOKEN_PLAY | TOKEN_PLAYERS;
				if (isRequestingRematch || isChangingGame) {
					shouldDeletePlayOnActivityCancel = true;
				}
				queryHandler.startQuery(TOKEN_PLAY, null, Plays.buildPlayUri(internalId), PlayBuilder.PLAY_PROJECTION, null, null, null);
			} else {
				// Starting a new play
				shouldDeletePlayOnActivityCancel = true;
				arePlayersCustomSorted = getIntent().getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false);
			}
			queryHandler.startQuery(TOKEN_COLORS, null, Games.buildColorsUri(gameId), new String[] { GameColors.COLOR }, null, null, null);
		}
	}

	@DebugLog
	private void onActionBarItemSelected(int itemId) {
		switch (itemId) {
			case R.id.menu_done:
				if (play != null && outstandingQueries == 0) {
					if (play.hasStarted()) {
						saveDraft(true);
						setResult(Activity.RESULT_OK);
						finish();
					} else {
						logPlay();
					}
				} else {
					cancel();
				}
				break;
			case R.id.menu_cancel:
				cancel();
				break;
		}
	}

	@DebugLog
	private void logPlay() {
		play.updateTimestamp = System.currentTimeMillis();
		play.deleteTimestamp = 0;
		play.dirtyTimestamp = System.currentTimeMillis();
		if (save()) {
			if (internalIdToDelete != BggContract.INVALID_ID) {
				PlayRepository playRepository = new PlayRepository((BggApplication) getApplication());
				playRepository.markAsDeleted(internalIdToDelete);
			}
			if (play.playId == 0 &&
				(DateUtils.isToday(play.dateInMillis) ||
					DateUtils.isToday(System.currentTimeMillis() - play.length * 60_000))
			) {
				PreferenceUtils.putLastPlayTime(prefs, System.currentTimeMillis());
				PreferenceUtils.putLastPlayLocation(prefs, play.location);
				PreferenceUtils.putLastPlayPlayers(prefs, play.getPlayers());
			}
			cancelNotification();
			triggerUpload();
			Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show();
		}
		setResult(Activity.RESULT_OK);
		finish();
	}

	@DebugLog
	private void saveDraft(boolean showToast) {
		if (play == null) return;
		play.dirtyTimestamp = System.currentTimeMillis();
		play.deleteTimestamp = 0;
		if (save()) {
			if (showToast) {
				Toast.makeText(this, R.string.msg_saving_draft, Toast.LENGTH_SHORT).show();
			}
			maybeShowNotification();
		}
	}

	private boolean save() {
		if (play == null) return false;
		shouldSaveOnPause = false;
		final View focusedView = recyclerView.findFocus();
		if (focusedView != null) focusedView.clearFocus();
		internalId = new PlayPersister(this).save(play, internalId, true);
		return true;
	}

	@DebugLog
	private void cancel() {
		shouldSaveOnPause = false;
		if (play == null) {
			setResult(RESULT_CANCELED);
			finish();
		} else if (play.equals(originalPlay)) {
			if (shouldDeletePlayOnActivityCancel) {
				deletePlay();
			}
			setResult(RESULT_CANCELED);
			finish();
		} else {
			if (shouldDeletePlayOnActivityCancel) {
				DialogUtils.createDiscardDialog(this, R.string.play, true, true, this::deletePlay).show();
			} else {
				DialogUtils.createDiscardDialog(this, R.string.play, false).show();
			}
		}
	}

	private void deletePlay() {
		play.updateTimestamp = 0;
		play.deleteTimestamp = System.currentTimeMillis();
		play.dirtyTimestamp = 0;
		if (save()) {
			triggerUpload();
			cancelNotification();
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
			.setItems(array, (dialog, which) -> {
				Resources r = getResources();
				String selection = array[which].toString();

				if (selection.equals(r.getString(R.string.location))) {
					isUserShowingLocation = true;
					playAdapter.insertRow(R.layout.row_log_play_location);
				} else if (selection.equals(r.getString(R.string.length))) {
					isUserShowingLength = true;
					playAdapter.insertRow(R.layout.row_log_play_length);
				} else if (selection.equals(r.getString(R.string.quantity))) {
					isUserShowingQuantity = true;
					playAdapter.insertRow(R.layout.row_log_play_quantity);
				} else if (selection.equals(r.getString(R.string.incomplete))) {
					isUserShowingIncomplete = true;
					play.incomplete = true;
					playAdapter.insertRow(R.layout.row_log_play_incomplete);
				} else if (selection.equals(r.getString(R.string.noWinStats))) {
					isUserShowingNoWinStats = true;
					play.noWinStats = true;
					playAdapter.insertRow(R.layout.row_log_play_no_win_stats);
				} else if (selection.equals(r.getString(R.string.comments))) {
					isUserShowingComments = true;
					playAdapter.insertRow(R.layout.row_log_play_comments);
				} else if (selection.equals(r.getString(R.string.title_players))) {
					isUserShowingPlayers = true;
					playAdapter.insertRow(R.layout.row_log_play_add_player);
				}

				Bundle bundle = new Bundle();
				bundle.putString(Param.CONTENT_TYPE, "Play");
				bundle.putString(Param.ITEM_NAME, selection);
				firebaseAnalytics.logEvent("AddField", bundle);

				supportInvalidateOptionsMenu();
			}).show();
	}

	@DebugLog
	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<>();
		if (shouldHideLocation()) list.add(r.getString(R.string.location));
		if (shouldHideLength()) list.add(r.getString(R.string.length));
		if (shouldHideQuantity()) list.add(r.getString(R.string.quantity));
		if (shouldHideIncomplete()) list.add(r.getString(R.string.incomplete));
		if (shouldHideNoWinStats()) list.add(r.getString(R.string.noWinStats));
		if (shouldHideComments()) list.add(r.getString(R.string.comments));
		if (shouldHidePlayers()) list.add(r.getString(R.string.title_players));

		CharSequence[] array = {};
		array = list.toArray(array);
		return array;
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

		try (Cursor cursor = getContentResolver().query(Plays.buildPlayersByUniqueNameUri(),
			new String[] { PlayPlayers._ID, PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.DESCRIPTION,
				PlayPlayers.COUNT, PlayPlayers.UNIQUE_NAME }, selection, selectionArgs, PlayPlayers.SORT_BY_COUNT)) {
			while (cursor != null && cursor.moveToNext()) {
				String username = cursor.getString(1);
				String name = cursor.getString(2);
				if (!containsPlayer(username, name)) {
					userNames.add(username);
					names.add(name);
					descriptions.add(cursor.getString(3));
				}
			}
		}

		if (descriptions.size() == 0) {
			return false;
		}

		CharSequence[] array = {};
		addPlayersBuilder
			.setMultiChoiceItems(descriptions.toArray(array), null, (dialog, which, isChecked) -> {
				Player player = new Player();
				player.username = userNames.get(which);
				player.name = names.get(which);
				if (isChecked) {
					playersToAdd.add(player);
				} else {
					playersToAdd.remove(player);
				}
			}).create().show();

		return true;
	}

	static class LocationAdapter extends AutoCompleteAdapter {
		LocationAdapter(Context context) {
			super(context, Plays.LOCATION, Plays.buildLocationsUri(), PlayLocations.SORT_BY_SUM_QUANTITY, Plays.SUM_QUANTITY);
		}

		public String getDefaultSelection() {
			return Plays.LOCATION + "<>''"; //String.format("%1$s='' OR %1s$ IS NULL", Plays.LOCATION);
		}
	}

	@DebugLog

	private DialogInterface.OnClickListener addPlayersButtonClickListener() {
		return (dialog, which) -> {
			play.setPlayers(playersToAdd);
			if (!arePlayersCustomSorted) {
				play.pickStartPlayer(0);
			}
			playAdapter.notifyPlayersChanged();
			if (which == DialogInterface.BUTTON_NEUTRAL) {
				addNewPlayer();
			}
		};
	}

	private void autoSortPlayers() {
		arePlayersCustomSorted = false;
		play.pickStartPlayer(0);
		playAdapter.notifyPlayersChanged();
	}

	@DebugLog
	private void promptPickStartPlayer() {
		CharSequence[] array = createArrayOfPlayerDescriptions();
		new AlertDialog.Builder(this).setTitle(R.string.title_pick_start_player)
			.setItems(array, (dialog, which) -> {
				play.pickStartPlayer(which);
				notifyStartPlayer();
				playAdapter.notifyPlayersChanged();
			})
			.show();
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
	private void addNewPlayer() {
		Intent intent = new Intent();
		if (!arePlayersCustomSorted) {
			intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, play.getPlayerCount() + 1);
		}
		editPlayer(intent, REQUEST_ADD_PLAYER);
	}

	@DebugLog
	private void editPlayer(int position) {
		Player player = playAdapter.getPlayer(position);
		Intent intent = new Intent();
		intent.putExtra(LogPlayerActivity.KEY_PLAYER, player);
		intent.putExtra(LogPlayerActivity.KEY_END_PLAY, isRequestingToEndPlay);
		if (!arePlayersCustomSorted && player != null) {
			intent.putExtra(LogPlayerActivity.KEY_AUTO_POSITION, player.getSeat());
		}
		intent.putExtra(LogPlayerActivity.KEY_POSITION, position);
		editPlayer(intent, REQUEST_EDIT_PLAYER);
		playAdapter.notifyPlayerChanged(position);
	}

	@DebugLog
	private void editPlayer(Intent intent, int requestCode) {
		isLaunchingActivity = true;
		intent.setClass(LogPlayActivity.this, LogPlayerActivity.class);
		intent.putExtra(LogPlayerActivity.KEY_GAME_ID, play.gameId);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, play.gameName);
		intent.putExtra(LogPlayerActivity.KEY_IMAGE_URL, imageUrl);
		intent.putExtra(LogPlayerActivity.KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(LogPlayerActivity.KEY_HERO_IMAGE_URL, heroImageUrl);
		intent.putExtra(LogPlayerActivity.KEY_END_PLAY, isRequestingToEndPlay);
		intent.putExtra(LogPlayerActivity.KEY_FAB_COLOR, fabColor);
		List<String> colors = new ArrayList<>();
		for (Player player : play.getPlayers()) {
			colors.add(player.color);
		}
		intent.putExtra(LogPlayerActivity.KEY_USED_COLORS, colors.toArray());
		intent.putExtra(LogPlayerActivity.KEY_NEW_PLAYER, requestCode == REQUEST_ADD_PLAYER);
		startActivityForResult(intent, requestCode);
	}

	@DebugLog
	private void maybeShowNotification() {
		if (play != null && play.hasStarted() && internalId != BggContract.INVALID_ID) {
			NotificationUtils.launchPlayingNotification(this, internalId, play, thumbnailUrl, imageUrl, heroImageUrl);
		}
	}

	@DebugLog
	private void cancelNotification() {
		NotificationUtils.cancel(LogPlayActivity.this, NotificationUtils.TAG_PLAY_TIMER, internalId);
	}

	@Override
	public void onColorSelected(@NotNull String description, int color, int requestCode) {
		Player player = play.getPlayers().get(requestCode);
		player.color = description;
		playAdapter.notifyPlayerChanged(requestCode);
	}

	@Override
	public void onNumberPadDone(double output, int requestCode) {
		int position = requestCode / 2;
		Player player = play.getPlayers().get(position);
		if (requestCode % 2 == 0) {
			player.score = SCORE_FORMAT.format(output);
			double highScore = play.getHighScore();
			for (Player p : play.getPlayers()) {
				double score = StringUtils.parseDouble(p.score, Double.NaN);
				p.isWin = (score == highScore);
			}
			playAdapter.notifyPlayersChanged();
		} else {
			player.rating = output;
			playAdapter.notifyPlayerChanged(position);
		}
	}

	public class PlayAdapter extends RecyclerView.Adapter<PlayAdapter.PlayViewHolder> {
		private final LayoutInflater inflater;
		private final List<Integer> headerResources = new ArrayList<>();
		private final List<Integer> footerResources = new ArrayList<>();

		public PlayAdapter(Context context) {
			setHasStableIds(false);
			inflater = LayoutInflater.from(context);
			buildLayoutMap();
		}

		@Override
		public int getItemCount() {
			return play == null ? 1 : headerResources.size() + play.getPlayerCount() + footerResources.size();
		}

		@Override
		public int getItemViewType(int position) {
			if (position < headerResources.size()) {
				return headerResources.get(position);
			} else if (position >= headerResources.size() + play.getPlayerCount()) {
				return footerResources.get(position - headerResources.size() - play.getPlayerCount());
			}
			return R.layout.row_player;
		}

		@NonNull
		@Override
		public PlayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			switch (viewType) {
				case R.layout.row_log_play_header:
					return new HeaderViewHolder(parent);
				case R.layout.row_log_play_date:
					return new DateViewHolder(parent);
				case R.layout.row_log_play_location:
					return new LocationViewHolder(parent);
				case R.layout.row_log_play_length:
					return new LengthViewHolder(parent);
				case R.layout.row_log_play_quantity:
					return new QuantityViewHolder(parent);
				case R.layout.row_log_play_incomplete:
					return new IncompleteViewHolder(parent);
				case R.layout.row_log_play_no_win_stats:
					return new NoWinStatsViewHolder(parent);
				case R.layout.row_log_play_comments:
					return new CommentsViewHolder(parent);
				case R.layout.row_log_play_player_header:
					return new PlayerHeaderViewHolder(parent);
				case R.layout.row_player:
					return new PlayerViewHolder();
				case R.layout.row_log_play_add_player:
					return new AddPlayerViewHolder(parent);
			}
			return new HeaderViewHolder(parent);
		}

		@Override
		public void onBindViewHolder(@NonNull PlayViewHolder holder, int position) {
			if (holder.getItemViewType() == R.layout.row_player) {
				((PlayerViewHolder) holder).bind(position - headerResources.size());
			} else {
				holder.bind();
			}
		}

		public int getPlayerPosition(int adapterPosition) {
			return adapterPosition - headerResources.size();
		}

		@Nullable
		public Player getPlayer(int position) {
			return (play == null || position < 0 || play.getPlayerCount() <= position) ?
				null :
				play.getPlayers().get(position);
		}

		public void refresh() {
			buildLayoutMap();
			notifyDataSetChanged();
		}

		public void notifyPlayersChanged() {
			new Handler().post(
				() -> {
					notifyLayoutChanged(R.layout.row_log_play_player_header);
					notifyItemRangeChanged(headerResources.size(), play.getPlayerCount());
				});
			maybeShowNotification();
		}

		public void notifyPlayerChanged(final int playerPosition) {
			new Handler().post(
				() -> notifyItemChanged(headerResources.size() + playerPosition));
		}

		public void notifyPlayerAdded(final int playerPosition) {
			new Handler().post(
				() -> {
					notifyLayoutChanged(R.layout.row_log_play_player_header);
					final int position = headerResources.size() + playerPosition;
					notifyItemInserted(position);
					notifyItemRangeChanged(position + 1, play.getPlayerCount() - playerPosition - 1);
				});
			maybeShowNotification();
		}

		public void notifyPlayerRemoved(final int playerPosition) {
			new Handler().post(
				() -> {
					notifyLayoutChanged(R.layout.row_log_play_player_header);
					final int position = headerResources.size() + playerPosition;
					notifyItemRemoved(position);
					notifyItemRangeChanged(position + 1, play.getPlayerCount() - playerPosition);
				});
			maybeShowNotification();
		}

		private void buildLayoutMap() {
			headerResources.clear();
			headerResources.add(R.layout.row_log_play_header);
			headerResources.add(R.layout.row_log_play_date);
			if (!shouldHideLocation()) headerResources.add(R.layout.row_log_play_location);
			if (!shouldHideLength()) headerResources.add(R.layout.row_log_play_length);
			if (!shouldHideQuantity()) headerResources.add(R.layout.row_log_play_quantity);
			if (!shouldHideIncomplete()) headerResources.add(R.layout.row_log_play_incomplete);
			if (!shouldHideNoWinStats()) headerResources.add(R.layout.row_log_play_no_win_stats);
			if (!shouldHideComments()) headerResources.add(R.layout.row_log_play_comments);
			if (!shouldHidePlayers()) headerResources.add(R.layout.row_log_play_player_header);

			footerResources.clear();
			if (!shouldHidePlayers()) footerResources.add(R.layout.row_log_play_add_player);
		}

		public void insertRow(@LayoutRes int layoutResId) {
			final int position = findPositionOfNewItemType(layoutResId);
			if (position != -1) {
				new Handler().post(
					() -> notifyItemInserted(position));
				recyclerView.smoothScrollToPosition(position);
				// TODO focus field
			}
		}

		private int findPositionOfNewItemType(@LayoutRes int layoutResId) {
			if (!headerResources.contains(layoutResId) || footerResources.contains(layoutResId)) {
				// call this because we know the field was just shown - this is confusing and needs to be refactored
				buildLayoutMap();
				for (int i = 0; i < headerResources.size(); i++) {
					if (headerResources.get(i) == layoutResId) {
						return i;
					}
				}
				for (int i = 0; i < footerResources.size(); i++) {
					if (footerResources.get(i) == layoutResId) {
						return headerResources.size() + play.getPlayerCount() + i;
					}
				}
			}
			// it already exists, so it's not new
			return -1;
		}

		private void notifyLayoutChanged(@LayoutRes int layoutResId) {
			for (int i = 0; i < headerResources.size(); i++) {
				if (headerResources.get(i) == layoutResId) {
					final int position = i;
					new Handler().post(
						() -> notifyItemChanged(position));
					return;
				}
			}
			for (int i = 0; i < footerResources.size(); i++) {
				if (footerResources.get(i) == layoutResId) {
					final int position = headerResources.size() + play.getPlayerCount() + i;
					new Handler().post(
						() -> notifyItemChanged(position));
					return;
				}
			}
		}

		abstract class PlayViewHolder extends RecyclerView.ViewHolder {
			public PlayViewHolder(View itemView) {
				super(itemView);
			}

			public abstract void bind();
		}

		public class HeaderViewHolder extends PlayViewHolder {
			@BindView(R.id.header) TextView headerView;
			@BindView(R.id.thumbnail) ImageView thumbnailView;

			public HeaderViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_header, parent, false));
				ButterKnife.bind(this, itemView);
			}

			@Override
			public void bind() {
				headerView.setText(gameName);

				fabColor = ContextCompat.getColor(LogPlayActivity.this, R.color.accent);
				ImageUtils.safelyLoadImage(thumbnailView, imageUrl, thumbnailUrl, heroImageUrl, new ImageUtils.Callback() {
					@Override
					public void onSuccessfulImageLoad(Palette palette) {
						headerView.setBackgroundResource(R.color.black_overlay_light);

						fabColor = PaletteUtils.getIconSwatch(palette).getRgb();
						FloatingActionButtonUtils.colorize(fab, fabColor);
						fab.post(() -> fab.show());

						notifyLayoutChanged(R.layout.row_log_play_add_player);
					}

					@Override
					public void onFailedImageLoad() {
						fab.show();
					}
				});
			}
		}

		public class DateViewHolder extends PlayViewHolder implements OnDateSetListener {
			private static final String DATE_PICKER_DIALOG_TAG = "DATE_PICKER_DIALOG";
			private DatePickerDialogFragment datePickerFragment;
			@BindView(R.id.log_play_date) TextView dateButton;

			public DateViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_date, parent, false));
				ButterKnife.bind(this, itemView);
				datePickerFragment = (DatePickerDialogFragment) getSupportFragmentManager().findFragmentByTag(DATE_PICKER_DIALOG_TAG);
				if (datePickerFragment != null) {
					datePickerFragment.setOnDateSetListener(this);
				}
			}

			@Override
			public void bind() {
				if (play == null) return;
				dateButton.setText(play.getDateForDisplay(LogPlayActivity.this));
			}

			@OnClick(R.id.log_play_date)
			public void onDateClick() {
				final FragmentManager fragmentManager = getSupportFragmentManager();
				datePickerFragment = (DatePickerDialogFragment) fragmentManager.findFragmentByTag(DATE_PICKER_DIALOG_TAG);

				if (datePickerFragment == null) {
					datePickerFragment = new DatePickerDialogFragment();
					datePickerFragment.setOnDateSetListener(this);
				}

				fragmentManager.executePendingTransactions();
				datePickerFragment.setCurrentDateInMillis(play.dateInMillis);
				datePickerFragment.show(fragmentManager, DATE_PICKER_DIALOG_TAG);
			}

			@Override
			public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
				if (play != null) {
					play.setDate(year, month, dayOfMonth);
					notifyLayoutChanged(R.layout.row_log_play_date);
					refresh();
				}
			}
		}

		public class LocationViewHolder extends PlayViewHolder {
			@BindView(R.id.log_play_location) AutoCompleteTextView locationView;
			private boolean canEdit;

			public LocationViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_location, parent, false));
				ButterKnife.bind(this, itemView);
				locationView.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int before, int count) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void afterTextChanged(Editable s) {
						if (canEdit) {
							play.location = s.toString().trim();
						}
					}
				});
			}

			@Override
			public void bind() {
				if (locationView.getAdapter() == null) locationView.setAdapter(locationAdapter);
				if (play != null) {
					locationView.setTextKeepState(play.location);
					canEdit = true;
				}
			}
		}

		public class LengthViewHolder extends PlayViewHolder {
			@BindView(R.id.log_play_length) EditText lengthView;
			@BindView(R.id.log_play_length_root) View lengthViewRoot;
			@BindView(R.id.timer) Chronometer timerView;
			@BindView(R.id.timer_toggle) ImageView timerToggleView;
			private boolean canEdit;

			public LengthViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_length, parent, false));
				ButterKnife.bind(this, itemView);
			}

			@Override
			public void bind() {
				if (play != null) {
					lengthView.setTextKeepState((play.length == Play.LENGTH_DEFAULT) ? "" : String.valueOf(play.length));
					UIUtils.startTimerWithSystemTime(timerView, play.startTime);
					canEdit = true;
					if (play.hasStarted()) {
						lengthViewRoot.setVisibility(View.GONE);
						timerView.setVisibility(View.VISIBLE);
					} else {
						lengthViewRoot.setVisibility(View.VISIBLE);
						timerView.setVisibility(View.GONE);
					}
					if (play.hasStarted()) {
						timerToggleView.setEnabled(true);
						timerToggleView.setImageResource(R.drawable.ic_timer_off);
					} else if (DateUtils.isToday(play.dateInMillis + play.length * 60 * 1000)) {
						timerToggleView.setEnabled(true);
						timerToggleView.setImageResource(R.drawable.ic_timer);
					} else {
						timerToggleView.setEnabled(false);
					}
				}
			}

			@OnFocusChange(R.id.log_play_length)
			public void onLengthFocusChange(EditText v, boolean hasFocus) {
				if (canEdit && !hasFocus) {
					play.length = StringUtils.parseInt(v.getText().toString().trim());
				}
			}

			@DebugLog
			@OnClick(R.id.timer_toggle)
			public void onTimer() {
				if (play.hasStarted()) {
					isRequestingToEndPlay = true;
					logTimer("Off");
					play.end();
					bind();
					cancelNotification();
					if (play.length > 0) {
						UIUtils.finishingEditing(lengthView);
					}
				} else {
					if (play.length == 0) {
						startTimer();
					} else {
						DialogUtils.createThemedBuilder(LogPlayActivity.this)
							.setMessage(R.string.are_you_sure_timer_reset)
							.setPositiveButton(R.string.continue_, (dialog, which) -> resumeTimer())
							.setNegativeButton(R.string.reset, (dialog, which) -> startTimer())
							.setCancelable(true)
							.show();
					}
				}
			}

			@DebugLog
			private void startTimer() {
				logTimer("On");
				play.start();
				bind();
				maybeShowNotification();
			}

			@DebugLog
			private void resumeTimer() {
				logTimer("On");
				play.resume();
				bind();
				maybeShowNotification();
			}
		}

		public class QuantityViewHolder extends PlayViewHolder {
			@BindView(R.id.log_play_quantity) EditText quantityView;
			private boolean canEdit;

			public QuantityViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_quantity, parent, false));
				ButterKnife.bind(this, itemView);
			}

			@Override
			public void bind() {
				if (play != null) {
					quantityView.setTextKeepState((play.quantity == Play.QUANTITY_DEFAULT) ? "" : String.valueOf(play.quantity));
					canEdit = true;
				}
			}

			@OnFocusChange(R.id.log_play_quantity)
			public void onQuantityFocusChange(EditText v, boolean hasFocus) {
				if (canEdit && !hasFocus) {
					play.quantity = StringUtils.parseInt(v.getText().toString().trim(), 1);
				}
			}
		}

		public class IncompleteViewHolder extends PlayViewHolder {
			@BindView(R.id.log_play_incomplete) SwitchCompat incompleteView;

			public IncompleteViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_incomplete, parent, false));
				ButterKnife.bind(this, itemView);
			}

			@Override
			public void bind() {
				if (play != null) {
					incompleteView.setChecked(play.incomplete);
				}
			}

			@OnCheckedChanged(R.id.log_play_incomplete)
			public void onIncompleteCheckedChanged() {
				play.incomplete = incompleteView.isChecked();
			}
		}

		public class NoWinStatsViewHolder extends PlayViewHolder {
			@BindView(R.id.log_play_no_win_stats) SwitchCompat noWinStatsView;

			public NoWinStatsViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_no_win_stats, parent, false));
				ButterKnife.bind(this, itemView);
			}

			@Override
			public void bind() {
				if (play != null) {
					noWinStatsView.setChecked(play.noWinStats);
				}
			}

			@OnCheckedChanged(R.id.log_play_no_win_stats)
			public void onIncompleteCheckedChanged() {
				play.noWinStats = noWinStatsView.isChecked();
			}
		}

		public class CommentsViewHolder extends PlayViewHolder {
			@BindView(R.id.log_play_comments) EditText commentsView;
			private boolean canEdit;

			public CommentsViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_comments, parent, false));
				ButterKnife.bind(this, itemView);
			}

			@Override
			public void bind() {
				if (play != null) {
					commentsView.setTextKeepState(play.comments);
					canEdit = true;
				}
			}

			@OnFocusChange(R.id.log_play_comments)
			public void onCommentsFocusChange(EditText v, boolean hasFocus) {
				if (canEdit && !hasFocus) {
					play.comments = v.getText().toString().trim();
				}
			}
		}

		public class AddPlayerViewHolder extends PlayViewHolder {
			@BindView(R.id.add_players_button) Button addPlayersButton;

			public AddPlayerViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_add_player, parent, false));
				ButterKnife.bind(this, itemView);
			}

			@Override
			public void bind() {
				ViewCompat.setBackgroundTintList(addPlayersButton, ColorStateList.valueOf(fabColor));
			}

			@OnClick(R.id.add_players_button)
			public void onAddPlayerClicked() {
				if (PreferenceUtils.getEditPlayerPrompted(prefs)) {
					addPlayers(PreferenceUtils.getEditPlayer(prefs));
				} else {
					promptToEditPlayers();
				}
			}

			private void addPlayers(boolean editPlayer) {
				if (editPlayer) {
					if (!showPlayersToAddDialog()) {
						addNewPlayer();
					}
				} else {
					Player player = new Player();
					if (!arePlayersCustomSorted) {
						player.setSeat(play.getPlayerCount() + 1);
					}
					play.addPlayer(player);
					playAdapter.notifyPlayerAdded(play.getPlayerCount());
					recyclerView.smoothScrollToPosition(playAdapter.getItemCount());
				}
			}

			private void promptToEditPlayers() {
				new Builder(LogPlayActivity.this)
					.setTitle(R.string.pref_edit_player_prompt_title)
					.setMessage(R.string.pref_edit_player_prompt_message)
					.setCancelable(true)
					.setPositiveButton(R.string.pref_edit_player_prompt_positive, onPromptClickListener(true))
					.setNegativeButton(R.string.pref_edit_player_prompt_negative, onPromptClickListener(false))
					.create().show();
				PreferenceUtils.putEditPlayerPrompted(prefs);
			}

			@NonNull
			private OnClickListener onPromptClickListener(final boolean value) {
				return (dialog, which) -> {
					PreferenceUtils.putEditPlayer(prefs, value);
					addPlayers(value);
				};
			}
		}

		public class PlayerHeaderViewHolder extends PlayViewHolder {
			@BindView(R.id.assign_colors) View assignColorsButton;
			@BindView(R.id.log_play_players_label) TextView playerLabelView;

			public PlayerHeaderViewHolder(ViewGroup parent) {
				super(inflater.inflate(R.layout.row_log_play_player_header, parent, false));
				ButterKnife.bind(this, itemView);
			}

			@Override
			public void bind() {
				Resources r = getResources();
				int playerCount = play.getPlayerCount();
				playerLabelView.setText(playerCount <= 0 ?
					r.getString(R.string.title_players) :
					r.getString(R.string.title_players_with_count, playerCount));
				assignColorsButton.setEnabled(play != null && playerCount > 0);
			}

			@OnClick(R.id.player_sort)
			public void onPlayerSort(View view) {
				PopupMenu popup = new PopupMenu(LogPlayActivity.this, view);
				popup.inflate(!arePlayersCustomSorted && play.getPlayerCount() > 1 ? R.menu.log_play_player_sort : R.menu.log_play_player_sort_short);
				popup.setOnMenuItemClickListener(item -> {
					switch (item.getItemId()) {
						case R.id.menu_custom_player_order:
							if (arePlayersCustomSorted) {
								logPlayerOrder("NotCustom");
								if (play.hasStartingPositions() && play.arePlayersCustomSorted()) {
									DialogUtils.createConfirmationDialog(LogPlayActivity.this,
										R.string.are_you_sure_player_sort_custom_off,
										(dialog, which) -> autoSortPlayers(),
										R.string.sort)
										.show();
								} else {
									autoSortPlayers();
								}
							} else {
								logPlayerOrder("Custom");
								if (play.hasStartingPositions()) {
									Builder builder = new Builder(LogPlayActivity.this)
										.setMessage(R.string.message_custom_player_order)
										.setPositiveButton(R.string.keep, (dialog, which) -> {
											arePlayersCustomSorted = true;
											playAdapter.notifyPlayersChanged();
										})
										.setNegativeButton(R.string.clear, (dialog, which) -> {
											arePlayersCustomSorted = true;
											play.clearPlayerPositions();
											playAdapter.notifyPlayersChanged();
										})
										.setCancelable(true);
									builder.show();
								}
							}
							return true;
						case R.id.menu_pick_start_player:
							logPlayerOrder("Prompt");
							promptPickStartPlayer();
							return true;
						case R.id.menu_random_start_player:
							logPlayerOrder("RandomStarter");
							int newSeat = new Random().nextInt(play.getPlayerCount());
							play.pickStartPlayer(newSeat);
							playAdapter.notifyPlayersChanged();
							notifyStartPlayer();
							return true;
						case R.id.menu_random_player_order:
							logPlayerOrder("Random");
							play.randomizePlayerOrder();
							playAdapter.notifyPlayersChanged();
							notifyStartPlayer();
							return true;
					}
					return false;
				});
				popup.show();
			}

			@OnClick(R.id.assign_colors)
			public void onAssignColors() {
				if (play.hasColors()) {
					Builder builder = new Builder(LogPlayActivity.this)
						.setTitle(R.string.title_clear_colors)
						.setMessage(R.string.msg_clear_colors)
						.setCancelable(true)
						.setNegativeButton(R.string.keep, (dialog, which) -> TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, play)))
						.setPositiveButton(R.string.clear, (dialog, which) -> {
							for (Player player : play.getPlayers()) {
								player.color = "";
							}
							TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, play));
						});
					builder.show();
				} else {
					TaskUtils.executeAsyncTask(new ColorAssignerTask(LogPlayActivity.this, play));
				}
			}
		}

		class PlayerViewHolder extends PlayViewHolder {
			private final PlayerRow row;

			public PlayerViewHolder() {
				super(new PlayerRow(LogPlayActivity.this));
				row = (PlayerRow) itemView;
				row.setLayoutParams(new RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			}

			@Override
			public void bind() {
				//no-op
			}

			@SuppressLint("ClickableViewAccessibility")
			public void bind(final int position) {
				row.setAutoSort(!arePlayersCustomSorted);
				row.setPlayer(getPlayer(position));
				row.setNameListener(v -> editPlayer(position));
				row.setOnMoreListener(v -> {
					if (position < 0 ||
						position > play.getPlayers().size())
						return;
					final Player player = play.getPlayers().get(position);

					PopupMenu popup = new PopupMenu(LogPlayActivity.this, row.getMoreButton());
					popup.inflate(R.menu.log_play_player);
					popup.getMenu().findItem(R.id.new_).setChecked(player.isNew);
					popup.getMenu().findItem(R.id.win).setChecked(player.isWin);
					popup.setOnMenuItemClickListener(item -> {
						switch (item.getItemId()) {
							case R.id.new_:
								player.isNew = !item.isChecked();
								bind(position);
								return true;
							case R.id.win:
								player.isWin = !item.isChecked();
								bind(position);
								return true;
						}
						return false;
					});
					popup.show();
				});
				row.getDragHandle().setOnTouchListener(
					(v, event) -> {
						if (event.getAction() == MotionEvent.ACTION_DOWN) {
							itemTouchHelper.startDrag(PlayerViewHolder.this);
							return true;
						}
						return false;
					});
				row.setOnColorListener(v -> {
					final Player player = play.getPlayers().get(position);
					final ArrayList<String> usedColors = new ArrayList<>();
					for (Player p : play.getPlayers()) {
						if (p != player) usedColors.add(p.color);
					}
					ColorPickerWithListenerDialogFragment fragment = ColorPickerWithListenerDialogFragment.newInstance(gameColors, player.color, usedColors, position);
					fragment.show(getSupportFragmentManager(), "color_picker");
				});
				row.setOnRatingListener(v -> {
					final Player player = play.getPlayers().get(position);
					final NumberPadDialogFragment fragment = PlayRatingNumberPadDialogFragment.newInstance(
						position * 2 + 1,
						player.getRatingDescription(),
						player.color,
						player.getDescription()
					);
					DialogUtils.showFragment(LogPlayActivity.this, fragment, "rating_dialog");
				});
				row.setOnScoreListener(v -> {
					final Player player = play.getPlayers().get(position);
					final NumberPadDialogFragment fragment = ScoreNumberPadDialogFragment.newInstance(
						position * 2,
						player.score,
						player.color,
						player.getDescription());
					DialogUtils.showFragment(LogPlayActivity.this, fragment, "score_dialog");
				});
			}
		}
	}

	private void logPlayerOrder(String order) {
		Bundle bundle = new Bundle();
		bundle.putString("Order", order);
		firebaseAnalytics.logEvent("LogPlayPlayerOrder", bundle);
	}

	private void logTimer(String state) {
		Bundle bundle = new Bundle();
		bundle.putString("State", state);
		firebaseAnalytics.logEvent("LogPlayTimer", bundle);
	}
}
