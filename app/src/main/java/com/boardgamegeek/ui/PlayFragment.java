package com.boardgamegeek.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlayDeletedEvent;
import com.boardgamegeek.events.PlaySentEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.DialogUtils.OnDiscardListener;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.fabric.PlayManipulationEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ShareEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class PlayFragment extends ListFragment implements LoaderCallbacks<Cursor>, OnRefreshListener {
	private static final String KEY_ID = "ID";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final int PLAY_QUERY_TOKEN = 0x01;
	private static final int PLAYER_QUERY_TOKEN = 0x02;

	private long internalId = BggContract.INVALID_ID;
	private Play play = new Play();
	private String thumbnailUrl;
	private String imageUrl;
	private boolean isRefreshing;

	private Unbinder unbinder;
	private ListView playersView;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.progress) View progressContainer;
	@BindView(R.id.list_container) View listContainer;
	@BindView(R.id.empty) TextView emptyView;
	@BindView(R.id.thumbnail) ImageView thumbnailView;
	@BindView(R.id.header) TextView gameNameView;
	@BindView(R.id.play_date) TextView dateView;
	@BindView(R.id.play_quantity) TextView quantityView;
	@BindView(R.id.length_root) View lengthContainer;
	@BindView(R.id.play_length) TextView lengthView;
	@BindView(R.id.timer_root) View timerContainer;
	@BindView(R.id.timer) Chronometer timerView;
	@BindView(R.id.location_root) View locationContainer;
	@BindView(R.id.play_location) TextView locationView;
	@BindView(R.id.play_incomplete) View incompleteView;
	@BindView(R.id.play_no_win_stats) View noWinStatsView;
	@BindView(R.id.play_comments) TextView commentsView;
	@BindView(R.id.play_comments_label) View commentsLabel;
	@BindView(R.id.play_players_label) View playersLabel;
	@BindView(R.id.play_id) TextView playIdView;
	@BindView(R.id.pending_timestamp) TimestampView pendingTimestampView;
	@BindView(R.id.dirty_timestamp) TimestampView dirtyTimestampView;
	@BindView(R.id.sync_timestamp) TimestampView syncTimestampView;
	private PlayerAdapter adapter;
	@State boolean hasBeenNotified;

	final private OnScrollListener onScrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (swipeRefreshLayout != null) {
				int topRowVerticalPosition = (view == null || view.getChildCount() == 0) ? 0 : view.getChildAt(0).getTop();
				swipeRefreshLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
			}
		}
	};

	public static PlayFragment newInstance(long internalId, int gameId, String gameName, String imageUrl, String thumbnailUrl) {
		Bundle args = new Bundle();
		args.putLong(KEY_ID, internalId);
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);
		args.putString(KEY_IMAGE_URL, imageUrl);
		args.putString(KEY_THUMBNAIL_URL, thumbnailUrl);
		PlayFragment fragment = new PlayFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		internalId = intent.getLongExtra(KEY_ID, BggContract.INVALID_ID);
		if (internalId == BggContract.INVALID_ID) return;

		play = new Play(intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID), intent.getStringExtra(KEY_GAME_NAME));

		thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL);
		imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_play, container, false);

		playersView = rootView.findViewById(android.R.id.list);
		playersView.setHeaderDividersEnabled(false);
		playersView.setFooterDividersEnabled(false);

		playersView.addHeaderView(View.inflate(getActivity(), R.layout.header_play, null), null, false);
		playersView.addFooterView(View.inflate(getActivity(), R.layout.footer_play, null), null, false);

		unbinder = ButterKnife.bind(this, rootView);

		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.setOnRefreshListener(this);
			swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());
		}

		adapter = new PlayerAdapter();
		playersView.setAdapter(adapter);

		getLoaderManager().restartLoader(PLAY_QUERY_TOKEN, null, this);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnScrollListener(onScrollListener);
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (play != null && play.hasStarted()) {
			showNotification();
			hasBeenNotified = true;
		}
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.play, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		UIUtils.showMenuItem(menu, R.id.menu_send, play.dirtyTimestamp > 0);
		UIUtils.showMenuItem(menu, R.id.menu_discard, play.playId > 0 && play.dirtyTimestamp > 0);
		UIUtils.enableMenuItem(menu, R.id.menu_share, play.playId > 0);
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_discard:
				DialogUtils.createDiscardDialog(getActivity(), R.string.play, false, false, new OnDiscardListener() {
					public void onDiscard() {
						play.dirtyTimestamp = 0;
						play.updateTimestamp = 0;
						play.deleteTimestamp = 0;
						save("Discard");
					}
				}).show();
				return true;
			case R.id.menu_edit:
				PlayManipulationEvent.log("Edit", play.gameName);
				LogPlayActivity.editPlay(getActivity(), internalId, play.gameId, play.gameName, thumbnailUrl, imageUrl);
				return true;
			case R.id.menu_send:
				play.updateTimestamp = System.currentTimeMillis();
				save("Save");
				EventBus.getDefault().post(new PlaySentEvent());
				return true;
			case R.id.menu_delete: {
				DialogUtils.createThemedBuilder(getContext())
					.setMessage(R.string.are_you_sure_delete_play)
					.setPositiveButton(R.string.delete, new OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (play.hasStarted()) cancelNotification();
							play.end(); // this prevents the timer from reappearing
							play.deleteTimestamp = System.currentTimeMillis();
							save("Delete");
							EventBus.getDefault().post(new PlayDeletedEvent());
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.setCancelable(true)
					.show();
				return true;
			}
			case R.id.menu_rematch:
				PlayManipulationEvent.log("Rematch", play.gameName);
				LogPlayActivity.rematch(getContext(), internalId, play.gameId, play.gameName, thumbnailUrl, imageUrl);
				getActivity().finish(); // don't want to show the "old" play upon return
				return true;
			case R.id.menu_share:
				ActivityUtils.share(getActivity(), play.toShortDescription(getActivity()), play.toLongDescription(getActivity()), R.string.share_play_title);
				Answers.getInstance().logShare(new ShareEvent()
					.putContentType("Play")
					.putContentName(play.toShortDescription(getActivity()))
					.putContentId(String.valueOf(play.playId)));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(SyncPlaysByGameTask.CompletedEvent event) {
		if (play != null && event.getGameId() == play.gameId) {
			if (!TextUtils.isEmpty(event.getErrorMessage()) && PreferencesUtils.getSyncShowErrors(getContext())) {
				// TODO: 3/30/17 change to a snackbar (will need to change from a ListFragment)
				Toast.makeText(getContext(), event.getErrorMessage(), Toast.LENGTH_LONG).show();
			}
			updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	private void updateRefreshStatus(final boolean value) {
		isRefreshing = value;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}

	@OnClick(R.id.header_container)
	void viewGame() {
		GameActivity.start(getContext(), play.gameId, play.gameName);
	}

	@OnClick(R.id.timer_end)
	void onTimerClick() {
		LogPlayActivity.endPlay(getContext(), internalId, play.gameId, play.gameName, thumbnailUrl, imageUrl);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case PLAY_QUERY_TOKEN:
				loader = new CursorLoader(getActivity(), Plays.buildPlayUri(internalId), PlayBuilder.PLAY_PROJECTION, null, null, null);
				break;
			case PLAYER_QUERY_TOKEN:
				loader = new CursorLoader(getActivity(), Plays.buildPlayerUri(internalId), PlayBuilder.PLAYER_PROJECTION, null, null, null);
				break;
		}
		if (loader != null) {
			loader.setUpdateThrottle(1000);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		switch (loader.getId()) {
			case PLAY_QUERY_TOKEN:
				if (onPlayQueryComplete(cursor)) {
					showList();
				}
				break;
			case PLAYER_QUERY_TOKEN:
				PlayBuilder.addPlayers(cursor, play);
				playersLabel.setVisibility(play.getPlayers().size() == 0 ? View.GONE : View.VISIBLE);
				adapter.notifyDataSetChanged();
				maybeShowNotification();
				showList();
				break;
			default:
				if (cursor != null) {
					cursor.close();
				}
				break;
		}
	}

	private void showList() {
		progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		progressContainer.setVisibility(View.GONE);
		listContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
		listContainer.setVisibility(View.VISIBLE);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	/**
	 * @return true if the we're done loading
	 */
	private boolean onPlayQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			emptyView.setText(String.format(getResources().getString(R.string.empty_play), String.valueOf(internalId)));
			emptyView.setVisibility(View.VISIBLE);
			return true;
		}
		playersView.setVisibility(View.VISIBLE);
		emptyView.setVisibility(View.GONE);

		ImageUtils.safelyLoadImage(thumbnailView, imageUrl, new Callback() {
			@Override
			public void onSuccessfulImageLoad(Palette palette) {
				if (gameNameView != null && isAdded()) {
					gameNameView.setBackgroundResource(R.color.black_overlay_light);
				}
			}

			@Override
			public void onFailedImageLoad() {
			}
		});

		List<Player> players = play.getPlayers();
		play = PlayBuilder.fromCursor(cursor);
		play.setPlayers(players);

		gameNameView.setText(play.gameName);

		dateView.setText(play.getDateForDisplay(getActivity()));

		quantityView.setText(getResources().getQuantityString(R.plurals.times_suffix, play.quantity, play.quantity));
		quantityView.setVisibility((play.quantity == 1) ? View.GONE : View.VISIBLE);

		if (play.length > 0) {
			lengthContainer.setVisibility(View.VISIBLE);
			lengthView.setText(DateTimeUtils.describeMinutes(getActivity(), play.length));
			lengthView.setVisibility(View.VISIBLE);
			timerContainer.setVisibility(View.GONE);
			timerView.stop();
		} else if (play.hasStarted()) {
			lengthContainer.setVisibility(View.VISIBLE);
			lengthView.setVisibility(View.GONE);
			timerContainer.setVisibility(View.VISIBLE);
			UIUtils.startTimerWithSystemTime(timerView, play.startTime);
		} else {
			lengthContainer.setVisibility(View.GONE);
		}

		locationView.setText(play.location);
		locationContainer.setVisibility(TextUtils.isEmpty(play.location) ? View.GONE : View.VISIBLE);

		incompleteView.setVisibility(play.Incomplete() ? View.VISIBLE : View.GONE);
		noWinStatsView.setVisibility(play.NoWinStats() ? View.VISIBLE : View.GONE);

		commentsView.setText(play.comments);
		commentsView.setVisibility(TextUtils.isEmpty(play.comments) ? View.GONE : View.VISIBLE);
		commentsLabel.setVisibility(TextUtils.isEmpty(play.comments) ? View.GONE : View.VISIBLE);

		if (play.deleteTimestamp > 0) {
			pendingTimestampView.setVisibility(View.VISIBLE);
			pendingTimestampView.setFormat(R.string.delete_pending_prefix);
			pendingTimestampView.setTimestamp(play.deleteTimestamp);
		} else if (play.updateTimestamp > 0) {
			pendingTimestampView.setVisibility(View.VISIBLE);
			pendingTimestampView.setFormat(R.string.update_pending_prefix);
			pendingTimestampView.setTimestamp(play.updateTimestamp);
		} else {
			pendingTimestampView.setVisibility(View.GONE);
		}

		if (play.dirtyTimestamp > 0) {
			dirtyTimestampView.setFormat(getString(play.playId > 0 ? R.string.editing_prefix : R.string.draft_prefix));
			dirtyTimestampView.setTimestamp(play.dirtyTimestamp);
		} else {
			dirtyTimestampView.setVisibility(View.GONE);
		}

		if (play.playId > 0) {
			playIdView.setText(String.format(getResources().getString(R.string.play_id_prefix), String.valueOf(play.playId)));
		}

		syncTimestampView.setTimestamp(play.syncTimestamp);

		getActivity().supportInvalidateOptionsMenu();
		getLoaderManager().restartLoader(PLAYER_QUERY_TOKEN, null, this);

		if (play.playId > 0 &&
			(play.syncTimestamp == 0 || DateTimeUtils.howManyDaysOld(play.syncTimestamp) > AGE_IN_DAYS_TO_REFRESH)) {
			triggerRefresh();
		}

		return false;
	}

	private void maybeShowNotification() {
		if (play.hasStarted()) {
			showNotification();
		} else if (hasBeenNotified) {
			cancelNotification();
		}
	}

	private void showNotification() {
		NotificationUtils.launchPlayingNotification(getActivity(), internalId, play, thumbnailUrl, imageUrl);
	}

	private void cancelNotification() {
		NotificationUtils.cancel(getActivity(), NotificationUtils.TAG_PLAY_TIMER, internalId);
	}

	private void triggerRefresh() {
		if (!isRefreshing) {
			TaskUtils.executeAsyncTask(new SyncPlaysByGameTask(getContext(), play.gameId));
			updateRefreshStatus(true);
		}
	}

	private void save(String action) {
		PlayManipulationEvent.log(TextUtils.isEmpty(action) ? "Save" : action, play.gameName);
		new PlayPersister(getActivity()).save(play, internalId, false);
		triggerRefresh();
	}

	private class PlayerAdapter extends BaseAdapter {
		@Override
		public boolean isEnabled(int position) {
			return false;
		}

		@Override
		public int getCount() {
			return play.getPlayerCount();
		}

		@Override
		public Object getItem(int position) {
			return play.getPlayers().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, final View convertView, ViewGroup parent) {
			PlayerRow row = new PlayerRow(getActivity());
			row.setLayoutParams(new ListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			final Player player = (Player) getItem(position);
			row.setPlayer(player);
			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					BuddyActivity.start(getContext(), player.username, player.name);
				}
			});
			return row;
		}
	}
}
