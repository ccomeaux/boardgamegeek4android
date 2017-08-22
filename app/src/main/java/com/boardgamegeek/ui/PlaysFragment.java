package com.boardgamegeek.ui;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.events.PlaysFilterChangedEvent;
import com.boardgamegeek.events.PlaysSortChangedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.sorter.PlaysSorterFactory;
import com.boardgamegeek.sorter.Sorter;
import com.boardgamegeek.tasks.sync.SyncPlaysByDateTask;
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask;
import com.boardgamegeek.ui.model.PlayModel;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.fabric.FilterEvent;
import com.boardgamegeek.util.fabric.SortEvent;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.Target;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import timber.log.Timber;

public class PlaysFragment extends StickyHeaderListFragment
	implements LoaderCallbacks<Cursor>, MultiChoiceModeListener, OnDateSetListener {
	public static final String KEY_MODE = "MODE";
	public static final String KEY_MODE_VALUE = "MODE_VALUE";
	public static final int FILTER_TYPE_STATUS_ALL = -2;
	public static final int FILTER_TYPE_STATUS_UPDATE = 1;
	public static final int FILTER_TYPE_STATUS_DIRTY = 2;
	public static final int FILTER_TYPE_STATUS_DELETE = 3;
	public static final int FILTER_TYPE_STATUS_PENDING = 4;
	private static final int MODE_ALL = 0;
	private static final int MODE_GAME = 1;
	public static final int MODE_BUDDY = 2;
	public static final int MODE_PLAYER = 3;
	public static final int MODE_LOCATION = 4;
	private static final int PLAY_QUERY_TOKEN = 0x21;
	private static final int HELP_VERSION = 2;
	private PlayAdapter adapter;
	private Uri uri;
	private int gameId;
	private String gameName;
	private String thumbnailUrl;
	private String imageUrl;
	private boolean arePlayersCustomSorted;
	private String buddyName;
	private String playerName;
	private String locationName;
	private int filterType = FILTER_TYPE_STATUS_ALL;
	private Sorter sorter;
	private boolean hasAutoSyncTriggered;
	private int mode = MODE_ALL;
	private final LinkedHashSet<Integer> selectedPlaysPositions = new LinkedHashSet<>();
	private MenuItem sendMenuItem;
	private MenuItem editMenuItem;
	private ShowcaseView showcaseView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int sortType = PlaysSorterFactory.TYPE_DEFAULT;
		sorter = PlaysSorterFactory.create(getActivity(), sortType);

		uri = Plays.CONTENT_URI;
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		Uri uri = intent.getData();
		int iconColor = intent.getIntExtra(ActivityUtils.KEY_ICON_COLOR, 0);

		mode = MODE_ALL;
		gameId = BggContract.INVALID_ID;
		buddyName = "";
		playerName = "";
		locationName = "";

		if (uri != null) {
			if (Games.isGameUri(uri)) {
				mode = MODE_GAME;
			} else if (Buddies.isBuddyUri(uri)) {
				mode = MODE_BUDDY;
			}
		} else {
			mode = getArguments().getInt(PlaysFragment.KEY_MODE, mode);
		}
		showFab(mode == MODE_GAME);
		if (fabView != null && iconColor != 0) {
			fabView.setBackgroundTintList(ColorStateList.valueOf(iconColor));
		}

		switch (mode) {
			case MODE_GAME:
				gameId = Games.getGameId(uri);
				gameName = getArguments().getString(ActivityUtils.KEY_GAME_NAME);
				thumbnailUrl = getArguments().getString(ActivityUtils.KEY_THUMBNAIL_URL);
				imageUrl = getArguments().getString(ActivityUtils.KEY_IMAGE_URL);
				arePlayersCustomSorted = getArguments().getBoolean(ActivityUtils.KEY_CUSTOM_PLAYER_SORT);
				getLoaderManager().restartLoader(GameQuery._TOKEN, getArguments(), this);
				break;
			case MODE_BUDDY:
				buddyName = getArguments().getString(PlaysFragment.KEY_MODE_VALUE);
				this.uri = Plays.buildPlayersByPlayUri();
				break;
			case MODE_PLAYER:
				playerName = getArguments().getString(PlaysFragment.KEY_MODE_VALUE);
				this.uri = Plays.buildPlayersByPlayUri();
				break;
			case MODE_LOCATION:
				locationName = getArguments().getString(PlaysFragment.KEY_MODE_VALUE);
				break;
		}

		setEmptyText(getString(getEmptyStringResource()));
		requery();

		final StickyListHeadersListView listView = getListView();
		if (listView != null) {
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			listView.setMultiChoiceModeListener(this);
		}

		maybeShowHelp();
	}

	@Override
	public void onListItemClick(View view, int position, long id) {
		Cursor cursor = (Cursor) adapter.getItem(position);
		if (cursor != null) {
			long internalId = cursor.getInt(cursor.getColumnIndex(Plays._ID));
			PlayModel play = PlayModel.fromCursor(cursor, getActivity());
			EventBus.getDefault().postSticky(new PlaySelectedEvent(internalId, play.getGameId(), play.getName(), play.getThumbnailUrl(), play.getImageUrl()));
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		switch (filterType) {
			case FILTER_TYPE_STATUS_DIRTY:
				UIUtils.checkMenuItem(menu, R.id.menu_filter_in_progress);
				break;
			case FILTER_TYPE_STATUS_PENDING:
				UIUtils.checkMenuItem(menu, R.id.menu_filter_pending);
				break;
			case FILTER_TYPE_STATUS_ALL:
			default:
				UIUtils.checkMenuItem(menu, R.id.menu_filter_all);
				break;
		}
		if (sorter != null) {
			switch (sorter.getType()) {
				case PlaysSorterFactory.TYPE_PLAY_DATE:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_date);
					break;
				case PlaysSorterFactory.TYPE_PLAY_GAME:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_game);
					break;
				case PlaysSorterFactory.TYPE_PLAY_LENGTH:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_length);
					break;
				case PlaysSorterFactory.TYPE_PLAY_LOCATION:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_location);
					break;
				default:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_date);
					break;
			}
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String title = item.getTitle().toString();
		switch (item.getItemId()) {
			case R.id.menu_sort_date:
				setSort(PlaysSorterFactory.TYPE_PLAY_DATE);
				return true;
			case R.id.menu_sort_location:
				setSort(PlaysSorterFactory.TYPE_PLAY_LOCATION);
				return true;
			case R.id.menu_sort_game:
				setSort(PlaysSorterFactory.TYPE_PLAY_GAME);
				return true;
			case R.id.menu_sort_length:
				setSort(PlaysSorterFactory.TYPE_PLAY_LENGTH);
				return true;
			case R.id.menu_filter_all:
				filter(FILTER_TYPE_STATUS_ALL, title);
				return true;
			case R.id.menu_filter_in_progress:
				filter(FILTER_TYPE_STATUS_DIRTY, title);
				return true;
			case R.id.menu_filter_pending:
				filter(FILTER_TYPE_STATUS_PENDING, title);
				return true;
			case R.id.menu_refresh_on:
				DatePickerFragment datePickerFragment = new DatePickerFragment();
				datePickerFragment.setListener(this);
				datePickerFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
				return true;
			case R.id.menu_help:
				showHelp();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean dividerShown() {
		return true;
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(PlaySelectedEvent event) {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPlaysByDateTask.CompletedEvent event) {
		isSyncing(false);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPlaysByGameTask.CompletedEvent event) {
		if (mode == MODE_GAME && event.getGameId() == gameId) {
			isSyncing(false);
			if (!TextUtils.isEmpty(event.getErrorMessage())) {
				Toast.makeText(getContext(), event.getErrorMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected boolean isRefreshable() {
		return super.isRefreshable() || (mode == MODE_GAME);
	}

	@DebugLog
	private void showHelp() {
		final Builder builder = HelpUtils.getShowcaseBuilder(getActivity());
		if (builder != null) {
			showcaseView = builder
				.setContentText(R.string.help_plays)
				.setTarget(Target.NONE)
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						showcaseView.hide();
						HelpUtils.updateHelp(getContext(), HelpUtils.HELP_PLAYS_KEY, HELP_VERSION);
					}
				})
				.build();
			showcaseView.show();
		}
	}

	@DebugLog
	private void maybeShowHelp() {
		if (HelpUtils.shouldShowHelp(getContext(), HelpUtils.HELP_PLAYS_KEY, HELP_VERSION)) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					showHelp();
				}
			}, 100);
		}
	}

	private void requery() {
		if (mode == MODE_ALL || mode == MODE_LOCATION || mode == MODE_GAME) {
			getLoaderManager().restartLoader(SumQuery._TOKEN, getArguments(), this);
		} else if (mode == MODE_PLAYER || mode == MODE_BUDDY) {
			getLoaderManager().restartLoader(PlayerSumQuery._TOKEN, getArguments(), this);
		}
		getLoaderManager().restartLoader(PLAY_QUERY_TOKEN, getArguments(), this);
	}

	@SuppressWarnings("unused")
	@Subscribe(sticky = true)
	public void onEvent(PlaysSortChangedEvent event) {
		setSort(event.getType());
	}

	@SuppressWarnings("unused")
	@Subscribe(sticky = true)
	public void onEvent(PlaysFilterChangedEvent event) {
		filter(event.getType(), event.getDescription());
	}

	private void setSort(int sortType) {
		if (sorter != null && sortType == sorter.getType()) {
			return;
		}
		if (sortType == PlaysSorterFactory.TYPE_UNKNOWN) {
			sortType = PlaysSorterFactory.TYPE_DEFAULT;
		}
		SortEvent.log("Plays", String.valueOf(sortType));
		sorter = PlaysSorterFactory.create(getActivity(), sortType);
		resetScrollState();
		requery();
	}

	public static class DatePickerFragment extends DialogFragment {
		private OnDateSetListener listener;

		@Override
		@NonNull
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Calendar calendar = Calendar.getInstance();
			return new DatePickerDialog(getActivity(),
				listener,
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH));
		}

		public void setListener(OnDateSetListener listener) {
			this.listener = listener;
		}
	}

	@Override
	public void onDateSet(DatePicker view, int year, int month, int day) {
		isSyncing(true);
		String date = DateTimeUtils.formatDateForApi(year, month, day);
		TaskUtils.executeAsyncTask(new SyncPlaysByDateTask(getContext(), date));
	}

	private int getEmptyStringResource() {
		switch (mode) {
			case MODE_BUDDY:
				return R.string.empty_plays_buddy;
			case MODE_PLAYER:
				return R.string.empty_plays_player;
			case MODE_LOCATION:
				return R.string.empty_plays_location;
			case MODE_GAME:
				return R.string.empty_plays_game;
			case MODE_ALL:
			default:
				switch (filterType) {
					case FILTER_TYPE_STATUS_DIRTY:
						return R.string.empty_plays_draft;
					case FILTER_TYPE_STATUS_UPDATE:
						return R.string.empty_plays_update;
					case FILTER_TYPE_STATUS_DELETE:
						return R.string.empty_plays_delete;
					case FILTER_TYPE_STATUS_PENDING:
						return R.string.empty_plays_pending;
					case FILTER_TYPE_STATUS_ALL:
					default:
						if (PreferencesUtils.getSyncPlays(getActivity())) {
							return R.string.empty_plays_sync_off;
						} else {
							return R.string.empty_plays_sync_off;
						}
				}
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == PLAY_QUERY_TOKEN) {
			loader = new CursorLoader(getActivity(),
				uri,
				sorter == null ? PlayModel.PROJECTION : StringUtils.unionArrays(PlayModel.PROJECTION, sorter.getColumns()),
				selection(),
				selectionArgs(),
				sorter == null ? null : sorter.getOrderByClause());
			loader.setUpdateThrottle(2000);
		} else if (id == GameQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), Games.buildGameUri(gameId), GameQuery.PROJECTION, null, null, null);
			loader.setUpdateThrottle(0);
		} else if (id == SumQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), Plays.CONTENT_SIMPLE_URI, SumQuery.PROJECTION, selection(), selectionArgs(), null);
			loader.setUpdateThrottle(0);
		} else if (id == PlayerSumQuery._TOKEN) {
			Uri uri = Plays.buildPlayersByUniquePlayerUri();
			if (mode == MODE_BUDDY) {
				uri = Plays.buildPlayersByUniqueUserUri();
			}
			loader = new CursorLoader(getActivity(), uri, PlayerSumQuery.PROJECTION, selection(), selectionArgs(), null);
			loader.setUpdateThrottle(0);
		}
		return loader;
	}

	private String selection() {
		switch (mode) {
			case MODE_ALL:
				if (filterType == FILTER_TYPE_STATUS_ALL) {
					return null;
				} else if (filterType == FILTER_TYPE_STATUS_PENDING) {
					return String.format("%s>0 OR %s>0", Plays.DELETE_TIMESTAMP, Plays.UPDATE_TIMESTAMP);
				} else {
					return String.format("%s>0", Plays.DIRTY_TIMESTAMP);
				}
			case MODE_GAME:
				return Plays.OBJECT_ID + "=?";
			case MODE_BUDDY:
				return PlayPlayers.USER_NAME + "=?";
			case MODE_PLAYER:
				return PlayPlayers.USER_NAME + "=? AND play_players." + PlayPlayers.NAME + "=?";
			case MODE_LOCATION:
				return Plays.LOCATION + "=?";
		}
		return null;
	}

	private String[] selectionArgs() {
		switch (mode) {
			case MODE_ALL:
				if (filterType == FILTER_TYPE_STATUS_ALL) {
					return null;
				} else if (filterType == FILTER_TYPE_STATUS_PENDING) {
					return null;
				} else {
					return null;
				}
			case MODE_GAME:
				return new String[] { String.valueOf(gameId) };
			case MODE_BUDDY:
				return new String[] { buddyName };
			case MODE_PLAYER:
				return new String[] { buddyName, playerName };
			case MODE_LOCATION:
				return new String[] { locationName };
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == PLAY_QUERY_TOKEN) {
			if (adapter == null) {
				adapter = new PlayAdapter(getActivity());
				setListAdapter(adapter);
			}
			adapter.changeCursor(cursor);
			restoreScrollState();
			PlaysSortChangedEvent event;
			if (sorter == null) {
				event = new PlaysSortChangedEvent(PlaysSorterFactory.TYPE_UNKNOWN, "");
			} else {
				event = new PlaysSortChangedEvent(sorter.getType(), sorter.getDescription());
			}
			EventBus.getDefault().postSticky(event);
		} else if (token == GameQuery._TOKEN) {
			if (!hasAutoSyncTriggered && cursor != null && cursor.moveToFirst()) {
				hasAutoSyncTriggered = true;
				long updated = cursor.getLong(GameQuery.UPDATED_PLAYS);
				if (updated == 0 || DateTimeUtils.howManyDaysOld(updated) > 2) {
					triggerRefresh();
				}
			}
		} else if (token == SumQuery._TOKEN) {
			int count = 0;
			if (cursor != null && cursor.moveToFirst()) {
				count = cursor.getInt(SumQuery.TOTAL_COUNT);
			}
			EventBus.getDefault().postSticky(new PlaysCountChangedEvent(count));
		} else if (token == PlayerSumQuery._TOKEN) {
			int count = 0;
			if (cursor != null && cursor.moveToFirst()) {
				count = cursor.getInt(PlayerSumQuery.SUM_QUANTITY);
			}
			EventBus.getDefault().postSticky(new PlaysCountChangedEvent(count));
		} else {
			Timber.d("Query complete, Not Actionable: %s", token);
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == PLAY_QUERY_TOKEN) {
			adapter.changeCursor(null);
		}
	}

	@Override
	protected int getSyncType() {
		return mode == MODE_GAME ? SyncService.FLAG_SYNC_NONE : SyncService.FLAG_SYNC_PLAYS;
	}

	@DebugLog
	@Override
	public void triggerRefresh() {
		if (isSyncing) return;
		switch (mode) {
			case MODE_ALL:
			case MODE_BUDDY:
			case MODE_PLAYER:
			case MODE_LOCATION:
				SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS);
				break;
			case MODE_GAME:
				isSyncing(true);
				SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS_UPLOAD);
				TaskUtils.executeAsyncTask(new SyncPlaysByGameTask(getContext(), gameId));
				break;
		}
	}

	@Override
	protected void onFabClicked() {
		Intent intent = ActivityUtils.createEditPlayIntent(getActivity(), gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
		startActivity(intent);
	}

	public void filter(int type, String description) {
		if (type != filterType && mode == MODE_ALL) {
			filterType = type;
			FilterEvent.log("Plays", String.valueOf(type));
			EventBus.getDefault().postSticky(new PlaysFilterChangedEvent(filterType, description));
			setEmptyText(getString(getEmptyStringResource()));
			requery();
		}
	}

	class PlayAdapter extends CursorAdapter implements StickyListHeadersAdapter {
		private final LayoutInflater inflater;

		public PlayAdapter(Context context) {
			super(context, null, false);
			inflater = getActivity().getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = inflater.inflate(R.layout.row_play, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			PlayModel play = PlayModel.fromCursor(cursor, getActivity());

			String info = PresentationUtils.describePlayDetails(getActivity(),
				mode != MODE_GAME ? play.getDate() : null,
				play.getLocation(), play.getQuantity(), play.getLength(), play.getPlayerCount());

			if (mode != MODE_GAME) {
				holder.title.setText(play.getName());
			} else {
				holder.title.setText(play.getDate());
			}
			PresentationUtils.setTextOrHide(holder.text1, info);
			PresentationUtils.setTextOrHide(holder.text2, play.getComments());

			int statusMessageId = 0;
			if (play.getDeleteTimestamp() > 0) {
				statusMessageId = R.string.sync_pending_delete;
			} else if (play.getUpdateTimestamp() > 0) {
				statusMessageId = R.string.sync_pending_update;
			} else if (play.getDirtyTimestamp() > 0) {
				if (play.getPlayId() > 0) {
					statusMessageId = R.string.sync_editing;
				} else {
					statusMessageId = R.string.sync_draft;
				}
			}
			if (statusMessageId == 0) {
				holder.status.setVisibility(View.GONE);
			} else {
				holder.status.setVisibility(View.VISIBLE);
				holder.status.setText(statusMessageId);
			}
		}

		@Override
		public long getHeaderId(int position) {
			if (position < 0 || sorter == null) {
				return 0;
			}
			return sorter.getHeaderId(getCursor(), position);
		}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = inflater.inflate(R.layout.row_header, parent, false);
				holder.text = convertView.findViewById(android.R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(sorter == null ? "" : sorter.getHeaderText(getCursor(), position));
			return convertView;
		}

		class ViewHolder {
			@BindView(android.R.id.title) TextView title;
			@BindView(android.R.id.text1) TextView text1;
			@BindView(android.R.id.text2) TextView text2;
			@BindView(android.R.id.message) TextView status;

			public ViewHolder(View view) {
				ButterKnife.bind(this, view);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}

	private interface GameQuery {
		int _TOKEN = 0x22;
		String[] PROJECTION = { Games.UPDATED_PLAYS };
		int UPDATED_PLAYS = 0;
	}

	private interface SumQuery {
		int _TOKEN = 0x23;
		String[] PROJECTION = { Plays.SUM_QUANTITY };
		int TOTAL_COUNT = 0;
	}

	private interface PlayerSumQuery {
		int _TOKEN = 0x24;
		String[] PROJECTION = { PlayPlayers.SUM_QUANTITY };

		int SUM_QUANTITY = 0;
	}

	// TODO Add support for share option

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.plays_context, menu);
		sendMenuItem = menu.findItem(R.id.menu_send);
		editMenuItem = menu.findItem(R.id.menu_edit);
		selectedPlaysPositions.clear();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if (!isAdded()) {
			return;
		}

		if (checked) {
			selectedPlaysPositions.add(position);
		} else {
			selectedPlaysPositions.remove(position);
		}

		int count = selectedPlaysPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_plays_selected, count, count));

		boolean allPending = true;
		for (int pos : selectedPlaysPositions) {
			Cursor cursor = (Cursor) adapter.getItem(pos);
			PlayModel play = PlayModel.fromCursor(cursor, getActivity());
			boolean pending = play.getDirtyTimestamp() > 0;
			allPending = allPending && pending;
		}

		sendMenuItem.setVisible(allPending);
		editMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (!selectedPlaysPositions.iterator().hasNext()) return false;
		switch (item.getItemId()) {
			case R.id.menu_send:
				mode.finish();
				new AlertDialog.Builder(getContext())
					.setMessage(getResources().getQuantityString(R.plurals.are_you_sure_send_play, selectedPlaysPositions.size()))
					.setCancelable(true)
					.setNegativeButton(R.string.cancel, null)
					.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							updateSelectedPlays(Plays.UPDATE_TIMESTAMP, System.currentTimeMillis());
						}
					})
					.show();
				return true;
			case R.id.menu_edit:
				mode.finish();
				Cursor cursor = (Cursor) adapter.getItem(selectedPlaysPositions.iterator().next());
				long internalId = CursorUtils.getLong(cursor, Plays._ID, BggContract.INVALID_ID);
				PlayModel play = PlayModel.fromCursor(cursor, getActivity());
				ActivityUtils.editPlay(getActivity(), internalId, play.getGameId(), play.getName(), play.getThumbnailUrl(), play.getImageUrl());
				return true;
			case R.id.menu_delete:
				mode.finish();
				new AlertDialog.Builder(getContext())
					.setMessage(getResources().getQuantityString(R.plurals.are_you_sure_delete_play, selectedPlaysPositions.size()))
					.setCancelable(true)
					.setNegativeButton(R.string.cancel, null)
					.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							updateSelectedPlays(Plays.DELETE_TIMESTAMP, System.currentTimeMillis());
						}
					})
					.show();

				return true;
		}
		return false;
	}

	private void updateSelectedPlays(String key, long value) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		for (int position : selectedPlaysPositions) {
			Cursor cursor = (Cursor) adapter.getItem(position);
			long internalId = CursorUtils.getLong(cursor, Plays._ID, BggContract.INVALID_ID);
			if (internalId != BggContract.INVALID_ID)
				batch.add(ContentProviderOperation
					.newUpdate(Plays.buildPlayUri(internalId))
					.withValue(key, value)
					.build());
		}
		ResolverUtils.applyBatch(getActivity(), batch);
		SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS_UPLOAD);
		selectedPlaysPositions.clear();
	}
}
