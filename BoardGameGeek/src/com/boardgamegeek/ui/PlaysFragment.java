package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.Calendar;
import java.util.LinkedHashSet;

import android.app.Activity;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.data.sort.PlaysSortDataFactory;
import com.boardgamegeek.data.sort.SortData;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

public class PlaysFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
	MultiChoiceModeListener {
	private static final String TAG = makeLogTag(PlaysFragment.class);
	private static final int MODE_ALL = 0;
	private static final int MODE_GAME = 1;
	private static final int MODE_BUDDY = 2;
	private static final String STATE_SORT_TYPE = "STATE_SORT_TYPE";
	private PlayAdapter mAdapter;
	private Uri mUri;
	private int mGameId;
	private String mBuddyName;
	private int mFilter;
	private SortData mSort;
	private boolean mAutoSyncTriggered;
	private int mMode = MODE_ALL;
	private int mSelectedPlayId;
	private LinkedHashSet<Integer> mSelectedPlaysPositions = new LinkedHashSet<Integer>();
	private MenuItem mSendMenuItem;
	private MenuItem mEditMenuItem;

	public interface Callbacks {
		public boolean onPlaySelected(int playId, int gameId, String gameName);

		public void onPlayCountChanged(int count);

		public void onSortChanged(String sortName);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onPlaySelected(int playId, int gameId, String gameName) {
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
				mGameId = Games.getGameId(uri);
				getLoaderManager().restartLoader(GameQuery._TOKEN, getArguments(), this);
			} else if (Buddies.isBuddyUri(uri)) {
				mMode = MODE_BUDDY;
				mBuddyName = getArguments().getString(BuddyUtils.KEY_BUDDY_NAME);
				mUri = Plays.buildPlayersUri();
			}
		}
		setEmptyText(getString(getEmptyStringResoure()));
		requery();

		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
	}

	private void requery() {
		if (mMode == MODE_ALL) {
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
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		if (cursor != null) {
			int playId = cursor.getInt(PlaysQuery.PLAY_ID);
			int gameId = cursor.getInt(PlaysQuery.GAME_ID);
			String gameName = cursor.getString(PlaysQuery.GAME_NAME);
			if (mCallbacks.onPlaySelected(playId, gameId, gameName)) {
				setSelectedPlayId(playId);
			}
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
			case R.id.menu_refresh:
				if (mAutoSyncTriggered) {
					Toast.makeText(getActivity(), R.string.msg_refresh_recent, Toast.LENGTH_LONG).show();
				} else {
					triggerRefresh();
				}
				return true;
			case R.id.menu_refresh_on:
				new DatePickerFragment().show(getActivity().getSupportFragmentManager(), "datePicker");
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
						return R.string.empty_plays;
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
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new PlayAdapter(getActivity());
			if (mMode == MODE_GAME) {
				mAdapter.setRowResId(R.layout.row_play_game);
			}
			setListAdapter(mAdapter);
		}

		int token = loader.getId();
		if (token == PlaysQuery._TOKEN) {
			mAdapter.changeCursor(cursor);
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
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
				SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS);
				break;
			case MODE_GAME:
				UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_PLAYS, mGameId, null);
				break;
		}
	}

	public void filter(int filter) {
		if (mMode == MODE_ALL) {
			mFilter = filter;
			setEmptyText(getString(getEmptyStringResoure()));
			requery();
		}
	}

	private class PlayAdapter extends CursorAdapter {
		private static final int STATE_UNKNOWN = 0;
		private static final int STATE_SECTIONED_CELL = 1;
		private static final int STATE_REGULAR_CELL = 2;

		private LayoutInflater mInflater;
		private int mRowResId = R.layout.row_play;
		private int[] mCellStates;
		private String mPreviousSection;
		private String mCurrentSection;

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
		public Cursor swapCursor(Cursor newCursor) {
			mCellStates = newCursor == null ? null : new int[newCursor.getCount()];
			return super.swapCursor(newCursor);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			boolean needSeparator = false;
			final int position = cursor.getPosition();
			mCurrentSection = mSort.getSectionText(cursor);
			switch (mCellStates[position]) {
				case STATE_SECTIONED_CELL:
					needSeparator = true;
					break;
				case STATE_REGULAR_CELL:
					needSeparator = false;
					break;
				case STATE_UNKNOWN:
				default:
					if (position == 0) {
						needSeparator = true;
					} else {
						cursor.moveToPosition(position - 1);
						mPreviousSection = mSort.getSectionText(cursor);
						if (!mPreviousSection.equals(mCurrentSection)) {
							needSeparator = true;
						}
						cursor.moveToPosition(position);
					}
					mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
					break;
			}

			if (needSeparator) {
				holder.separator.setText(mCurrentSection);
				holder.separator.setVisibility(View.VISIBLE);
			} else {
				holder.separator.setVisibility(View.GONE);
			}

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
			if (playerCount > 0 && mMode != MODE_BUDDY) {
				// TODO make this work for budddies
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
	}

	static class ViewHolder {
		TextView name;
		TextView date;
		TextView location;
		TextView status;
		TextView separator;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.list_name);
			date = (TextView) view.findViewById(R.id.list_date);
			location = (TextView) view.findViewById(R.id.list_location);
			status = (TextView) view.findViewById(R.id.list_status);
			separator = (TextView) view.findViewById(R.id.separator);
		}
	}

	private interface PlaysQuery {
		int _TOKEN = 0x21;
		String[] PROJECTION = { Plays._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, "COUNT(" + PlayPlayers.USER_ID + ")" };
		int PLAY_ID = 1;
		int DATE = 2;
		int GAME_NAME = 3;
		int GAME_ID = 4;
		int LOCATION = 5;
		int QUANTITY = 6;
		int LENGTH = 7;
		int SYNC_STATUS = 8;
		int PLAYER_COUNT = 9;
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
					cursor.getInt(PlaysQuery.GAME_ID), cursor.getString(PlaysQuery.GAME_NAME));
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
