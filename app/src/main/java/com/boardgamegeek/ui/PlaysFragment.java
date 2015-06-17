package com.boardgamegeek.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.events.PlaysFilterChangedEvent;
import com.boardgamegeek.events.PlaysSortChangedEvent;
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
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

import java.util.Calendar;
import java.util.LinkedHashSet;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class PlaysFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
	MultiChoiceModeListener {
	public static final String KEY_MODE = "MODE";
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_USER_NAME = "USER_NAME";
	public static final String KEY_LOCATION = "LOCATION";
	private static final int MODE_ALL = 0;
	private static final int MODE_GAME = 1;
	public static final int MODE_BUDDY = 2;
	public static final int MODE_PLAYER = 3;
	public static final int MODE_LOCATION = 4;
	private PlayAdapter mAdapter;
	private Uri mUri;
	private int mGameId;
	private String mBuddyName;
	private String mPlayerName;
	private String mLocation;
	private int mFilterType = Play.SYNC_STATUS_ALL;
	private Sorter mSorter;
	private boolean mAutoSyncTriggered;
	private int mMode = MODE_ALL;
	private int mSelectedPlayId;
	private LinkedHashSet<Integer> mSelectedPlaysPositions = new LinkedHashSet<>();
	private MenuItem mSendMenuItem;
	private MenuItem mEditMenuItem;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int sortType = PlaysSorterFactory.TYPE_DEFAULT;
		mSorter = PlaysSorterFactory.create(sortType, getActivity());

		mUri = Plays.CONTENT_URI;
		Uri uri = UIUtils.fragmentArgumentsToIntent(getArguments()).getData();
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

		switch (mMode) {
			case MODE_GAME:
				mGameId = Games.getGameId(uri);
				getLoaderManager().restartLoader(GameQuery._TOKEN, getArguments(), this);
				break;
			case MODE_BUDDY:
				mBuddyName = getArguments().getString(ActivityUtils.KEY_BUDDY_NAME);
				mUri = Plays.buildPlayersUri();
				break;
			case MODE_PLAYER:
				mBuddyName = getArguments().getString(KEY_USER_NAME);
				mPlayerName = getArguments().getString(KEY_PLAYER_NAME);
				mUri = Plays.buildPlayersUri();
				break;
			case MODE_LOCATION:
				mLocation = getArguments().getString(KEY_LOCATION);
				break;
		}

		setEmptyText(getString(getEmptyStringResource()));
		requery();

		ActionMode.setMultiChoiceMode(getListView().getWrappedList(), getActivity(), this);
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	public void onListItemClick(View view, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		if (cursor != null) {
			int playId = cursor.getInt(PlaysQuery.PLAY_ID);
			int gameId = cursor.getInt(PlaysQuery.GAME_ID);
			String gameName = cursor.getString(PlaysQuery.GAME_NAME);
			String thumbnailUrl = cursor.getString(PlaysQuery.THUMBNAIL_URL);
			String imageUrl = cursor.getString(PlaysQuery.IMAGE_URL);
			EventBus.getDefault().postSticky(new PlaySelectedEvent(playId, gameId, gameName, thumbnailUrl, imageUrl));
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		DrawerActivity activity = (DrawerActivity) getActivity();
		boolean showOptions = true;
		if (activity != null) {
			showOptions = !activity.isDrawerOpen();
		}
		showMenuItemSafely(menu, R.id.menu_sort, showOptions);
		showMenuItemSafely(menu, R.id.menu_filter, showOptions);
		showMenuItemSafely(menu, R.id.menu_refresh, showOptions);
		showMenuItemSafely(menu, R.id.menu_refresh_on, showOptions);
		if (showOptions) {
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
		}
		super.onPrepareOptionsMenu(menu);
	}

	private static void showMenuItemSafely(Menu menu, int resourceId, boolean visible) {
		MenuItem menuItem = menu.findItem(resourceId);
		if (menuItem != null) {
			menuItem.setVisible(visible);
		}
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
			case R.id.menu_refresh:
				triggerRefresh();
				return true;
			case R.id.menu_refresh_on:
				new DatePickerFragment().show(getActivity().getSupportFragmentManager(), "datePicker");
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean dividerShown() {
		return true;
	}

	@DebugLog
	public void onEvent(PlaySelectedEvent event) {
		mSelectedPlayId = event.playId;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private void requery() {
		if (mMode == MODE_ALL || mMode == MODE_LOCATION || mMode == MODE_GAME) {
			getLoaderManager().restartLoader(SumQuery._TOKEN, getArguments(), this);
		} else if (mMode == MODE_PLAYER || mMode == MODE_BUDDY) {
			getLoaderManager().restartLoader(PlayerSumQuery._TOKEN, getArguments(), this);
		}
		getLoaderManager().restartLoader(PlaysQuery._TOKEN, getArguments(), this);
	}

	public void onEvent(PlaysSortChangedEvent event) {
		setSort(event.type);
	}

	public void onEvent(PlaysFilterChangedEvent event) {
		filter(event.type, event.description);
	}

	private void setSort(int sortType) {
		if (mSorter != null && sortType == mSorter.getType()) {
			return;
		}
		if (sortType == PlaysSorterFactory.TYPE_UNKNOWN) {
			sortType = PlaysSorterFactory.TYPE_DEFAULT;
		}
		mSorter = PlaysSorterFactory.create(sortType, getActivity());
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
		if (id == PlaysQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), mUri, mSorter == null ? PlaysQuery.PROJECTION
				: StringUtils.unionArrays(PlaysQuery.PROJECTION, mSorter.getColumns()), selection(), selectionArgs(),
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
		if (token == PlaysQuery._TOKEN) {
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
		if (loader.getId() == PlaysQuery._TOKEN) {
			mAdapter.changeCursor(null);
		}
	}

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

	public void filter(int type, String description) {
		if (type != mFilterType && mMode == MODE_ALL) {
			mFilterType = type;
			EventBus.getDefault().postSticky(new PlaysFilterChangedEvent(mFilterType, description));
			setEmptyText(getString(getEmptyStringResource()));
			requery();
		}
	}

	class PlayAdapter extends CursorAdapter implements StickyListHeadersAdapter {
		private LayoutInflater mInflater;
		private String mOn;
		private String mTimes;
		private String mAt;
		private String mFor;

		public PlayAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();

			mOn = context.getString(R.string.on);
			mTimes = context.getString(R.string.times);
			mAt = context.getString(R.string.at);
			mFor = context.getString(R.string.for_);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_play, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			int playId = cursor.getInt(PlaysQuery.PLAY_ID);
			UIUtils.setActivatedCompat(view, playId == mSelectedPlayId);

			String name = cursor.getString(PlaysQuery.GAME_NAME);
			String date = CursorUtils.getFormattedDateAbbreviated(cursor, getActivity(), PlaysQuery.DATE);
			String location = cursor.getString(PlaysQuery.LOCATION);
			int quantity = cursor.getInt(PlaysQuery.QUANTITY);
			int length = cursor.getInt(PlaysQuery.LENGTH);
			int playerCount = cursor.getInt(PlaysQuery.PLAYER_COUNT);
			String comments = cursor.getString(PlaysQuery.COMMENTS).trim();
			int status = cursor.getInt(PlaysQuery.SYNC_STATUS);

			String info = "";
			if (mMode != MODE_GAME) {
				info += mOn + " " + date + " ";
			}
			if (quantity > 1) {
				info += quantity + " " + mTimes + " ";
			}
			if (!TextUtils.isEmpty(location)) {
				info += mAt + " " + location + " ";
			}
			if (length > 0) {
				info += mFor + " " + DateTimeUtils.formatMinutes(length) + " ";
			}
			if (playerCount > 0) {
				info += getResources().getQuantityString(R.plurals.player_description, playerCount, playerCount);
			}
			info = info.trim();

			int messageId = 0;
			if (status != Play.SYNC_STATUS_SYNCED) {
				if (status == Play.SYNC_STATUS_IN_PROGRESS) {
					if (Play.hasBeenSynced(playId)) {
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
				holder.title.setText(name);
			} else {
				holder.title.setText(date);
			}
			if (TextUtils.isEmpty(info)) {
				holder.text1.setVisibility(View.GONE);
			} else {
				holder.text1.setVisibility(View.VISIBLE);
				holder.text1.setText(info);
			}
			if (TextUtils.isEmpty(comments.trim())) {
				holder.text2.setVisibility(View.GONE);
			} else {
				holder.text2.setVisibility(View.VISIBLE);
				holder.text2.setText(comments.trim());
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
				convertView = mInflater.inflate(R.layout.row_header, parent, false);
				holder.text = (TextView) convertView.findViewById(android.R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(mSorter == null ? "" : mSorter.getHeaderText(getCursor(), position));
			return convertView;
		}

		class ViewHolder {
			@InjectView(android.R.id.title) TextView title;
			@InjectView(android.R.id.text1) TextView text1;
			@InjectView(android.R.id.text2) TextView text2;
			@InjectView(android.R.id.message) TextView status;

			public ViewHolder(View view) {
				ButterKnife.inject(this, view);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}

	private interface PlaysQuery {
		int _TOKEN = 0x21;
		String[] PROJECTION = { Plays._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, Plays.PLAYER_COUNT, Games.THUMBNAIL_URL,
			Games.IMAGE_URL, Plays.COMMENTS };
		int PLAY_ID = 1;
		int DATE = 2;
		int GAME_NAME = 3;
		int GAME_ID = 4;
		int LOCATION = 5;
		int QUANTITY = 6;
		int LENGTH = 7;
		int SYNC_STATUS = 8;
		int PLAYER_COUNT = 9;
		int THUMBNAIL_URL = 10;
		int IMAGE_URL = 11;
		int COMMENTS = 12;
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
			boolean pending = cursor.getInt(PlaysQuery.SYNC_STATUS) == Play.SYNC_STATUS_IN_PROGRESS;
			allPending = allPending && pending;
		}

		mSendMenuItem.setVisible(allPending);
		mEditMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (mSelectedPlaysPositions == null || !mSelectedPlaysPositions.iterator().hasNext()) {
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
				ActivityUtils.editPlay(getActivity(), cursor.getInt(PlaysQuery.PLAY_ID),
					cursor.getInt(PlaysQuery.GAME_ID), cursor.getString(PlaysQuery.GAME_NAME),
					cursor.getString(PlaysQuery.THUMBNAIL_URL), cursor.getString(PlaysQuery.IMAGE_URL));
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
			resolver.update(Plays.buildPlayUri(cursor.getInt(PlaysQuery.PLAY_ID)), values, null, null);
		}
		mSelectedPlaysPositions.clear();
		SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}
}
