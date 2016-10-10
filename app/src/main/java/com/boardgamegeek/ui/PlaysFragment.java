package com.boardgamegeek.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
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

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.events.PlaysFilterChangedEvent;
import com.boardgamegeek.events.PlaysSortChangedEvent;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.sorter.PlaysSorterFactory;
import com.boardgamegeek.sorter.Sorter;
import com.boardgamegeek.ui.model.PlayModel;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.LinkedHashSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import timber.log.Timber;

public class PlaysFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor>, MultiChoiceModeListener {
	public static final String KEY_MODE = "MODE";
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_USER_NAME = "USER_NAME";
	public static final String KEY_LOCATION = "LOCATION";
	private static final int MODE_ALL = 0;
	private static final int MODE_GAME = 1;
	public static final int MODE_BUDDY = 2;
	public static final int MODE_PLAYER = 3;
	public static final int MODE_LOCATION = 4;
	private static final int PLAY_QUERY_TOKEN = 0x21;
	private static final int HELP_VERSION = 2;
	private PlayAdapter mAdapter;
	private Uri mUri;
	private int mGameId;
	private String mGameName;
	private String mThumbnailUrl;
	private String mImageUrl;
	private boolean mCustomPlayerSort;
	private String mBuddyName;
	private String mPlayerName;
	private String mLocation;
	private int mFilterType = Play.SYNC_STATUS_ALL;
	private Sorter mSorter;
	private boolean mAutoSyncTriggered;
	private int mMode = MODE_ALL;
	private int mSelectedPlayId;
	private final LinkedHashSet<Integer> mSelectedPlaysPositions = new LinkedHashSet<>();
	private MenuItem mSendMenuItem;
	private MenuItem mEditMenuItem;
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
		mSorter = PlaysSorterFactory.create(getActivity(), sortType);

		mUri = Plays.CONTENT_URI;
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		Uri uri = intent.getData();
		int iconColor = intent.getIntExtra(ActivityUtils.KEY_ICON_COLOR, 0);
		mMode = MODE_ALL;
		mGameId = BggContract.INVALID_ID;
		mBuddyName = "";
		if (uri != null) {
			if (Games.isGameUri(uri)) {
				mMode = MODE_GAME;
			} else if (Buddies.isBuddyUri(uri)) {
				mMode = MODE_BUDDY;
			}
		} else {
			mMode = getArguments().getInt(PlaysFragment.KEY_MODE, mMode);
		}
		showFab(mMode == MODE_GAME);
		if (fabView != null && iconColor != 0) {
			fabView.setBackgroundTintList(ColorStateList.valueOf(iconColor));
		}

		switch (mMode) {
			case MODE_GAME:
				mGameId = Games.getGameId(uri);
				mGameName = getArguments().getString(ActivityUtils.KEY_GAME_NAME);
				mThumbnailUrl = getArguments().getString(ActivityUtils.KEY_THUMBNAIL_URL);
				mImageUrl = getArguments().getString(ActivityUtils.KEY_IMAGE_URL);
				mCustomPlayerSort = getArguments().getBoolean(ActivityUtils.KEY_CUSTOM_PLAYER_SORT);
				getLoaderManager().restartLoader(GameQuery._TOKEN, getArguments(), this);
				break;
			case MODE_BUDDY:
				mBuddyName = getArguments().getString(ActivityUtils.KEY_BUDDY_NAME);
				mUri = Plays.buildPlayersByPlayUri();
				break;
			case MODE_PLAYER:
				mBuddyName = getArguments().getString(KEY_USER_NAME);
				mPlayerName = getArguments().getString(KEY_PLAYER_NAME);
				mUri = Plays.buildPlayersByPlayUri();
				break;
			case MODE_LOCATION:
				mLocation = getArguments().getString(KEY_LOCATION);
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
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		if (cursor != null) {
			PlayModel play = PlayModel.fromCursor(cursor, getActivity());
			EventBus.getDefault().postSticky(new PlaySelectedEvent(play.getPlayId(), play.getGameId(), play.getName(), play.getThumbnailUrl(), play.getImageUrl()));
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		switch (mFilterType) {
			case Play.SYNC_STATUS_IN_PROGRESS:
				checkMenuItemSafely(menu, R.id.menu_filter_in_progress);
				break;
			case Play.SYNC_STATUS_PENDING:
				checkMenuItemSafely(menu, R.id.menu_filter_pending);
				break;
			case Play.SYNC_STATUS_ALL:
			default:
				checkMenuItemSafely(menu, R.id.menu_filter_all);
				break;
		}
		if (mSorter != null) {
			switch (mSorter.getType()) {
				case PlaysSorterFactory.TYPE_PLAY_DATE:
					checkMenuItemSafely(menu, R.id.menu_sort_date);
					break;
				case PlaysSorterFactory.TYPE_PLAY_GAME:
					checkMenuItemSafely(menu, R.id.menu_sort_game);
					break;
				case PlaysSorterFactory.TYPE_PLAY_LENGTH:
					checkMenuItemSafely(menu, R.id.menu_sort_length);
					break;
				case PlaysSorterFactory.TYPE_PLAY_LOCATION:
					checkMenuItemSafely(menu, R.id.menu_sort_location);
					break;
				default:
					checkMenuItemSafely(menu, R.id.menu_sort_date);
					break;
			}
		}
		super.onPrepareOptionsMenu(menu);
	}

	private static void checkMenuItemSafely(Menu menu, int resourceId) {
		MenuItem menuItem = menu.findItem(resourceId);
		if (menuItem != null) {
			menuItem.setChecked(true);
		}
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
				filter(Play.SYNC_STATUS_ALL, title);
				return true;
			case R.id.menu_filter_in_progress:
				filter(Play.SYNC_STATUS_IN_PROGRESS, title);
				return true;
			case R.id.menu_filter_pending:
				filter(Play.SYNC_STATUS_PENDING, title);
				return true;
			case R.id.menu_refresh_on:
				new DatePickerFragment().show(getActivity().getSupportFragmentManager(), "datePicker");
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
		mSelectedPlayId = event.getPlayId();
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(UpdateEvent event) {
		isSyncing((event.getType() == UpdateService.SYNC_TYPE_GAME_PLAYS) || (event.getType() == UpdateService.SYNC_TYPE_PLAYS_DATE));
	}

	@SuppressWarnings({ "UnusedParameters", "unused" })
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(UpdateCompleteEvent event) {
		isSyncing(false);
	}

	@Override
	protected boolean isRefreshable() {
		return super.isRefreshable() || (mMode == MODE_GAME);
	}

	@DebugLog
	private void showHelp() {
		showcaseView = HelpUtils.getShowcaseBuilder(getActivity())
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
		if (mMode == MODE_ALL || mMode == MODE_LOCATION || mMode == MODE_GAME) {
			getLoaderManager().restartLoader(SumQuery._TOKEN, getArguments(), this);
		} else if (mMode == MODE_PLAYER || mMode == MODE_BUDDY) {
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
		if (mSorter != null && sortType == mSorter.getType()) {
			return;
		}
		if (sortType == PlaysSorterFactory.TYPE_UNKNOWN) {
			sortType = PlaysSorterFactory.TYPE_DEFAULT;
		}
		mSorter = PlaysSorterFactory.create(getActivity(), sortType);
		resetScrollState();
		requery();
	}

	public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
		// HACK prevent onDateSet from firing twice
		private boolean alreadyCalled = false;

		@Override
		@NonNull
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Calendar calendar = Calendar.getInstance();
			return new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH));
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			if (alreadyCalled) {
				return;
			}
			alreadyCalled = true;

			String date = DateTimeUtils.formatDateForApi(year, month, day);
			UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_PLAYS_DATE, date);
		}
	}

	private int getEmptyStringResource() {
		switch (mMode) {
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
				switch (mFilterType) {
					case Play.SYNC_STATUS_IN_PROGRESS:
						return R.string.empty_plays_draft;
					case Play.SYNC_STATUS_PENDING_UPDATE:
						return R.string.empty_plays_update;
					case Play.SYNC_STATUS_PENDING_DELETE:
						return R.string.empty_plays_delete;
					case Play.SYNC_STATUS_PENDING:
						return R.string.empty_plays_pending;
					case Play.SYNC_STATUS_ALL:
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
				mUri,
				mSorter == null ? PlayModel.PROJECTION : StringUtils.unionArrays(PlayModel.PROJECTION, mSorter.getColumns()),
				selection(),
				selectionArgs(),
				mSorter == null ? null : mSorter.getOrderByClause());
			loader.setUpdateThrottle(2000);
		} else if (id == GameQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), Games.buildGameUri(mGameId), GameQuery.PROJECTION, null, null,
				null);
			loader.setUpdateThrottle(0);
		} else if (id == SumQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), Plays.CONTENT_SIMPLE_URI, SumQuery.PROJECTION, selection(),
				selectionArgs(), null);
			loader.setUpdateThrottle(0);
		} else if (id == PlayerSumQuery._TOKEN) {
			Uri uri = Plays.buildPlayersByUniquePlayerUri();
			if (mMode == MODE_BUDDY) {
				uri = Plays.buildPlayersByUniqueUserUri();
			}
			loader = new CursorLoader(getActivity(), uri, PlayerSumQuery.PROJECTION, selection(), selectionArgs(), null);
			loader.setUpdateThrottle(0);
		}
		return loader;
	}

	private String selection() {
		switch (mMode) {
			case MODE_ALL:
				if (mFilterType == Play.SYNC_STATUS_ALL) {
					return null;
				} else if (mFilterType == Play.SYNC_STATUS_PENDING) {
					return Plays.SYNC_STATUS + "=? OR " + Plays.SYNC_STATUS + "=?";
				} else {
					return Plays.SYNC_STATUS + "=?";
				}
			case MODE_GAME:
				return PlayItems.OBJECT_ID + "=?";
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
		switch (mMode) {
			case MODE_ALL:
				if (mFilterType == Play.SYNC_STATUS_ALL) {
					return null;
				} else if (mFilterType == Play.SYNC_STATUS_PENDING) {
					return new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE),
						String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) };
				} else {
					return new String[] { String.valueOf(mFilterType) };
				}
			case MODE_GAME:
				return new String[] { String.valueOf(mGameId) };
			case MODE_BUDDY:
				return new String[] { mBuddyName };
			case MODE_PLAYER:
				return new String[] { mBuddyName, mPlayerName };
			case MODE_LOCATION:
				return new String[] { mLocation };
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
			if (mAdapter == null) {
				mAdapter = new PlayAdapter(getActivity());
				setListAdapter(mAdapter);
			}
			mAdapter.changeCursor(cursor);
			restoreScrollState();
			PlaysSortChangedEvent event;
			if (mSorter == null) {
				event = new PlaysSortChangedEvent(PlaysSorterFactory.TYPE_UNKNOWN, "");
			} else {
				event = new PlaysSortChangedEvent(mSorter.getType(), mSorter.getDescription());
			}
			EventBus.getDefault().postSticky(event);
		} else if (token == GameQuery._TOKEN) {
			if (!mAutoSyncTriggered && cursor != null && cursor.moveToFirst()) {
				mAutoSyncTriggered = true;
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
			Timber.d("Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (loader.getId() == PLAY_QUERY_TOKEN) {
			mAdapter.changeCursor(null);
		}
	}

	@DebugLog
	@Override
	protected int getSyncType() {
		return mMode == MODE_GAME ? SyncService.FLAG_SYNC_NONE : SyncService.FLAG_SYNC_PLAYS;
	}

	@DebugLog
	@Override
	public void triggerRefresh() {
		switch (mMode) {
			case MODE_ALL:
			case MODE_BUDDY:
			case MODE_PLAYER:
			case MODE_LOCATION:
				SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS);
				break;
			case MODE_GAME:
				UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_PLAYS, mGameId);
				break;
		}
	}

	@Override
	protected void onFabClicked() {
		Intent intent = ActivityUtils.createEditPlayIntent(getActivity(), 0, mGameId, mGameName, mThumbnailUrl, mImageUrl);
		intent.putExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, mCustomPlayerSort);
		startActivity(intent);
	}

	public void filter(int type, String description) {
		if (type != mFilterType && mMode == MODE_ALL) {
			mFilterType = type;
			EventBus.getDefault().postSticky(new PlaysFilterChangedEvent(mFilterType, description));
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

			UIUtils.setActivatedCompat(view, play.getPlayId() == mSelectedPlayId);

			String info = PresentationUtils.describePlayDetails(getActivity(),
				mMode != MODE_GAME ? play.getDate() : null,
				play.getLocation(), play.getQuantity(), play.getLength(), play.getPlayerCount());

			int messageId = 0;
			int status = play.getStatus();
			if (status != Play.SYNC_STATUS_SYNCED) {
				if (status == Play.SYNC_STATUS_IN_PROGRESS) {
					if (Play.hasBeenSynced(play.getPlayId())) {
						messageId = R.string.sync_editing;
					} else {
						messageId = R.string.sync_draft;
					}
				} else if (status == Play.SYNC_STATUS_PENDING_UPDATE) {
					messageId = R.string.sync_pending_update;
				} else if (status == Play.SYNC_STATUS_PENDING_DELETE) {
					messageId = R.string.sync_pending_delete;
				}
			}

			if (mMode != MODE_GAME) {
				holder.title.setText(play.getName());
			} else {
				holder.title.setText(play.getDate());
			}
			if (TextUtils.isEmpty(info)) {
				holder.text1.setVisibility(View.GONE);
			} else {
				holder.text1.setVisibility(View.VISIBLE);
				holder.text1.setText(info);
			}
			if (TextUtils.isEmpty(play.getComments())) {
				holder.text2.setVisibility(View.GONE);
			} else {
				holder.text2.setVisibility(View.VISIBLE);
				holder.text2.setText(play.getComments());
			}
			if (messageId == 0) {
				holder.status.setVisibility(View.GONE);
			} else {
				holder.status.setVisibility(View.VISIBLE);
				holder.status.setText(messageId);
			}
		}

		@Override
		public long getHeaderId(int position) {
			if (position < 0 || mSorter == null) {
				return 0;
			}
			return mSorter.getHeaderId(getCursor(), position);
		}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = inflater.inflate(R.layout.row_header, parent, false);
				holder.text = (TextView) convertView.findViewById(android.R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(mSorter == null ? "" : mSorter.getHeaderText(getCursor(), position));
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
		String[] PROJECTION = { "SUM(" + Plays.QUANTITY + ")" };
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
		mSendMenuItem = menu.findItem(R.id.menu_send);
		mEditMenuItem = menu.findItem(R.id.menu_edit);
		mSelectedPlaysPositions.clear();
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
			mSelectedPlaysPositions.add(position);
		} else {
			mSelectedPlaysPositions.remove(position);
		}

		int count = mSelectedPlaysPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_plays_selected, count, count));

		boolean allPending = true;
		for (int pos : mSelectedPlaysPositions) {
			Cursor cursor = (Cursor) mAdapter.getItem(pos);
			PlayModel play = PlayModel.fromCursor(cursor, getActivity());
			boolean pending = play.getStatus() == Play.SYNC_STATUS_IN_PROGRESS;
			allPending = allPending && pending;
		}

		mSendMenuItem.setVisible(allPending);
		mEditMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (!mSelectedPlaysPositions.iterator().hasNext()) {
			return false;
		}
		switch (item.getItemId()) {
			case R.id.menu_send:
				mode.finish();
				DialogUtils.createConfirmationDialog(getActivity(),
					getResources().getQuantityString(R.plurals.are_you_sure_send_play, mSelectedPlaysPositions.size()),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							updateSyncStatus(Play.SYNC_STATUS_PENDING_UPDATE);
						}
					}).show();
				return true;
			case R.id.menu_edit:
				mode.finish();
				Cursor cursor = (Cursor) mAdapter.getItem(mSelectedPlaysPositions.iterator().next());
				PlayModel play = PlayModel.fromCursor(cursor, getActivity());
				ActivityUtils.editPlay(getActivity(), play.getPlayId(), play.getGameId(), play.getName(), play.getThumbnailUrl(), play.getImageUrl());
				return true;
			case R.id.menu_delete:
				mode.finish();
				DialogUtils.createConfirmationDialog(
					getActivity(),
					getResources()
						.getQuantityString(R.plurals.are_you_sure_delete_play, mSelectedPlaysPositions.size()),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							updateSyncStatus(Play.SYNC_STATUS_PENDING_DELETE);
						}
					}).show();
				return true;
		}
		return false;
	}

	private void updateSyncStatus(int status) {
		ContentResolver resolver = getActivity().getContentResolver();
		ContentValues values = new ContentValues();
		values.put(Plays.SYNC_STATUS, status);
		for (int position : mSelectedPlaysPositions) {
			Cursor cursor = (Cursor) mAdapter.getItem(position);
			PlayModel play = PlayModel.fromCursor(cursor, getActivity());
			resolver.update(Plays.buildPlayUri(play.getPlayId()), values, null, null);
		}
		mSelectedPlaysPositions.clear();
		SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}
}
