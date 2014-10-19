package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.Calendar;
import java.util.LinkedHashSet;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.sorter.PlaysSortDataFactory;
import com.boardgamegeek.sorter.Sorter;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

public class PlaysFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
	MultiChoiceModeListener {
	private static final String TAG = makeLogTag(PlaysFragment.class);
	public static final String KEY_MODE = "MODE";
	public static final String KEY_PLAYER_NAME = "PLAYER_NAME";
	public static final String KEY_USER_NAME = "USER_NAME";
	public static final String KEY_LOCATION = "LOCATION";
	private static final int MODE_ALL = 0;
	private static final int MODE_GAME = 1;
	public static final int MODE_BUDDY = 2;
	public static final int MODE_PLAYER = 3;
	public static final int MODE_LOCATION = 4;
	private static final String STATE_SORT_TYPE = "STATE_SORT_TYPE";
	private PlayAdapter mAdapter;
	private Uri mUri;
	private int mGameId;
	private String mBuddyName;
	private String mPlayerName;
	private String mLocation;
	private int mFilter = Play.SYNC_STATUS_ALL;
	private Sorter mSort;
	private boolean mAutoSyncTriggered;
	private int mMode = MODE_ALL;
	private int mSelectedPlayId;
	private LinkedHashSet<Integer> mSelectedPlaysPositions = new LinkedHashSet<Integer>();
	private MenuItem mSendMenuItem;
	private MenuItem mEditMenuItem;

	public interface Callbacks {
		public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl);

		public void onPlayCountChanged(int count);

		public void onSortChanged(String sortName);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
			return true;
		}

		@Override
		public void onPlayCountChanged(int count) {
		}

		public void onSortChanged(String sortName) {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int sortType = PlaysSortDataFactory.TYPE_DEFAULT;
		if (savedInstanceState != null) {
			sortType = savedInstanceState.getInt(STATE_SORT_TYPE);
		}
		mSort = PlaysSortDataFactory.create(sortType, getActivity());

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
				mBuddyName = getArguments().getString(BuddyUtils.KEY_BUDDY_NAME);
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

		setEmptyText(getString(getEmptyStringResoure()));
		requery();

		ActionMode.setMultiChoiceMode(getListView().getWrappedList(), getActivity(), this);
	}

	private void requery() {
		if (mMode == MODE_ALL || mMode == MODE_LOCATION) {
			getLoaderManager().restartLoader(SumQuery._TOKEN, getArguments(), this);
		}
		getLoaderManager().restartLoader(PlaysQuery._TOKEN, getArguments(), this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}
		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_SORT_TYPE, mSort == null ? PlaysSortDataFactory.TYPE_UNKNOWN : mSort.getType());
		super.onSaveInstanceState(outState);
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
			if (mCallbacks.onPlaySelected(playId, gameId, gameName, thumbnailUrl, imageUrl)) {
				setSelectedPlayId(playId);
			}
		}
	}

	@Override
	public void onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		DrawerActivity activity = (DrawerActivity) getActivity();
		boolean showOptions = true;
		if (activity != null) {
			showOptions = !activity.isDrawerOpen();
		}
		showMenuItemSafely(menu, R.id.menu_sort, showOptions);
		showMenuItemSafely(menu, R.id.menu_refresh, showOptions);
		showMenuItemSafely(menu, R.id.menu_refresh_on, showOptions);
		if (showOptions) {
			switch (mSort.getType()) {
				case PlaysSortDataFactory.TYPE_PLAY_DATE:
					checkMenuItemSafely(menu, R.id.menu_sort_date);
					break;
				case PlaysSortDataFactory.TYPE_PLAY_GAME:
					checkMenuItemSafely(menu, R.id.menu_sort_game);
					break;
				case PlaysSortDataFactory.TYPE_PLAY_LENGTH:
					checkMenuItemSafely(menu, R.id.menu_sort_length);
					break;
				case PlaysSortDataFactory.TYPE_PLAY_LOCATION:
					checkMenuItemSafely(menu, R.id.menu_sort_location);
					break;
				default:
					checkMenuItemSafely(menu, R.id.menu_sort_date);
					break;
			}
		}
		super.onPrepareOptionsMenu(menu);
	}

	private void showMenuItemSafely(com.actionbarsherlock.view.Menu menu, int resourceId, boolean visible) {
		com.actionbarsherlock.view.MenuItem menuItem = menu.findItem(resourceId);
		if (menuItem != null) {
			menuItem.setVisible(visible);
		}
	}

	private void checkMenuItemSafely(com.actionbarsherlock.view.Menu menu, int resourceId) {
		com.actionbarsherlock.view.MenuItem menuItem = menu.findItem(resourceId);
		if (menuItem != null) {
			menuItem.setChecked(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_sort_date:
				setSort(PlaysSortDataFactory.TYPE_PLAY_DATE);
				return true;
			case R.id.menu_sort_location:
				setSort(PlaysSortDataFactory.TYPE_PLAY_LOCATION);
				return true;
			case R.id.menu_sort_game:
				setSort(PlaysSortDataFactory.TYPE_PLAY_GAME);
				return true;
			case R.id.menu_sort_length:
				setSort(PlaysSortDataFactory.TYPE_PLAY_LENGTH);
				return true;
			case R.id.menu_refresh:
				triggerRefresh();
				return true;
			case R.id.menu_refresh_on:
				new DatePickerFragment().show(getActivity().getSupportFragmentManager(), "datePicker");
				return true;
			case R.id.menu_h_index:
				int hIndex = PreferencesUtils.getHIndex(getActivity());
				Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.sync_notification_title_h_index).setNegativeButton(R.string.close, null);
				if (hIndex != -1) {
					builder.setMessage(StringUtils.boldSecondString(getString(R.string.message_h_index),
						String.valueOf(hIndex), "\n\n" + getString(R.string.message_h_index_description, hIndex)));
				} else {
					builder.setMessage(R.string.message_h_index_missing);
				}
				builder.create().show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setSort(int sortType) {
		if (sortType == mSort.getType()) {
			return;
		}
		if (sortType == PlaysSortDataFactory.TYPE_UNKNOWN) {
			sortType = PlaysSortDataFactory.TYPE_DEFAULT;
		}
		mSort = PlaysSortDataFactory.create(sortType, getActivity());
		resetScrollState();
		requery();
	}

	public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
		// HACK prevent onDateSet from firing twice
		private boolean alreadyCalled = false;

		@Override
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
			UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_PLAYS_DATE, date, null);
		}
	}

	private void setSelectedPlayId(int playId) {
		mSelectedPlayId = playId;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private int getEmptyStringResoure() {
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
				switch (mFilter) {
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
			loader = new CursorLoader(getActivity(), mUri, mSort == null ? PlaysQuery.PROJECTION
				: StringUtils.unionArrays(PlaysQuery.PROJECTION, mSort.getColumns()), selection(), selectionArgs(),
				mSort == null ? null : mSort.getOrderByClause());
			if (loader != null) {
				loader.setUpdateThrottle(2000);
			}
		} else if (id == GameQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), Games.buildGameUri(mGameId), GameQuery.PROJECTION, null, null,
				null);
			if (loader != null) {
				loader.setUpdateThrottle(0);
			}
		} else if (id == SumQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), Plays.CONTENT_SIMPLE_URI, SumQuery.PROJECTION, selection(),
				selectionArgs(), null);
			if (loader != null) {
				loader.setUpdateThrottle(0);
			}
		}
		return loader;
	}

	private String selection() {
		switch (mMode) {
			case MODE_ALL:
				if (mFilter == Play.SYNC_STATUS_ALL) {
					return null;
				} else if (mFilter == Play.SYNC_STATUS_PENDING) {
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
				if (mFilter == Play.SYNC_STATUS_ALL) {
					return null;
				} else if (mFilter == Play.SYNC_STATUS_PENDING) {
					return new String[] { String.valueOf(Play.SYNC_STATUS_PENDING_UPDATE),
						String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) };
				} else {
					return new String[] { String.valueOf(mFilter) };
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
				if (mMode == MODE_GAME) {
					mAdapter.setRowResId(R.layout.row_play_game);
				}
				setListAdapter(mAdapter);
			}
			mAdapter.changeCursor(cursor);
			restoreScrollState();
			mCallbacks.onSortChanged(mSort == null ? "" : mSort.getDescription());
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
			mCallbacks.onPlayCountChanged(count);
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
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
				UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_PLAYS, mGameId, null);
				break;
		}
	}

	public void filter(int filter) {
		if (filter != mFilter && mMode == MODE_ALL) {
			mFilter = filter;
			setEmptyText(getString(getEmptyStringResoure()));
			requery();
		}
	}

	private class PlayAdapter extends CursorAdapter implements StickyListHeadersAdapter {
		private LayoutInflater mInflater;
		private int mRowResId = R.layout.row_play;

		private String mTimes;
		private String mAt;
		private String mFor;

		public PlayAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();

			mTimes = context.getString(R.string.times);
			mAt = context.getString(R.string.at);
			mFor = context.getString(R.string.for_);
		}

		public void setRowResId(int resId) {
			mRowResId = resId;
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(mRowResId, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			int playId = cursor.getInt(PlaysQuery.PLAY_ID);
			UIUtils.setActivatedCompat(view, playId == mSelectedPlayId);

			holder.date.setText(CursorUtils.getFormettedDateAbbreviated(cursor, getActivity(), PlaysQuery.DATE));
			holder.name.setText(cursor.getString(PlaysQuery.GAME_NAME));
			String location = cursor.getString(PlaysQuery.LOCATION);
			int quantity = cursor.getInt(PlaysQuery.QUANTITY);
			int length = cursor.getInt(PlaysQuery.LENGTH);
			int playerCount = cursor.getInt(PlaysQuery.PLAYER_COUNT);

			String info = "";
			if (quantity > 1) {
				info += quantity + " " + mTimes + " ";
			}
			if (!TextUtils.isEmpty(location)) {
				info += mAt + " " + location + " ";
			}
			if (length > 0) {
				int hours = length / 60;
				int minutes = length % 60;
				info += mFor + " " + String.format("%d:%02d", hours, minutes) + " ";
			}
			if (playerCount > 0) {
				info += getResources().getQuantityString(R.plurals.player_description, playerCount, playerCount);
			}
			holder.location.setText(info.trim());

			int status = cursor.getInt(PlaysQuery.SYNC_STATUS);
			if (status != Play.SYNC_STATUS_SYNCED) {
				int messageId = 0;
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
				holder.status.setText(messageId);
				holder.status.setVisibility(View.VISIBLE);
			} else {
				holder.status.setVisibility(View.GONE);
			}
		}

		@Override
		public long getHeaderId(int position) {
			if (position < 0) {
				return 0;
			}
			return mSort.getHeaderId(getCursor(), position);
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
			holder.text.setText(mSort.getHeaderText(getCursor(), position));
			return convertView;
		}

		class ViewHolder {
			TextView name;
			TextView date;
			TextView location;
			TextView status;

			public ViewHolder(View view) {
				name = (TextView) view.findViewById(R.id.list_name);
				date = (TextView) view.findViewById(R.id.list_date);
				location = (TextView) view.findViewById(R.id.list_location);
				status = (TextView) view.findViewById(R.id.list_status);
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
			Games.IMAGE_URL };
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
		switch (item.getItemId()) {
			case R.id.menu_send:
				mode.finish();
				ActivityUtils.createConfirmationDialog(getActivity(),
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
				ActivityUtils.createConfirmationDialog(
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
