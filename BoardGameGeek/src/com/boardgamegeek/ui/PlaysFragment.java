package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

public class PlaysFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
	MultiChoiceModeListener {
	private static final String TAG = makeLogTag(PlaysFragment.class);
	private static final int MODE_ALL = 0;
	private static final int MODE_GAME = 1;
	private static final int MODE_BUDDY = 2;
	private PlayAdapter mAdapter;
	private Uri mUri;
	private int mGameId;
	private String mBuddyName;
	private int mFilter;
	private boolean mAutoSyncTriggered;
	private int mMode = MODE_ALL;
	private int mSelectedPlayId;
	private LinkedHashSet<Integer> mSelectedPlaysPositions = new LinkedHashSet<Integer>();
	private MenuItem mSendMenuItem;
	private MenuItem mEditMenuItem;

	public interface Callbacks {
		public boolean onPlaySelected(int playId, int gameId, String gameName);

		public void onPlayCountChanged(int count);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onPlaySelected(int playId, int gameId, String gameName) {
			return true;
		}

		@Override
		public void onPlayCountChanged(int count) {
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
		getLoaderManager().restartLoader(PlaysQuery._TOKEN, getArguments(), this);

		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
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
		if (item.getItemId() == R.id.menu_refresh) {
			if (mAutoSyncTriggered) {
				Toast.makeText(getActivity(), R.string.msg_refresh_recent, Toast.LENGTH_LONG).show();
			} else {
				triggerRefresh();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
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
			switch (mMode) {
				case MODE_ALL:
					if (mFilter == Play.SYNC_STATUS_ALL) {
						loader = new CursorLoader(getActivity(), mUri, PlaysQuery.PROJECTION, null, null, null);
					} else {
						loader = new CursorLoader(getActivity(), mUri, PlaysQuery.PROJECTION, Plays.SYNC_STATUS + "=?",
							new String[] { String.valueOf(mFilter) }, null);
					}
					break;
				case MODE_GAME:
					loader = new CursorLoader(getActivity(), mUri, PlaysQuery.PROJECTION, PlayItems.OBJECT_ID + "=?",
						new String[] { String.valueOf(mGameId) }, null);
					break;
				case MODE_BUDDY:
					loader = new CursorLoader(getActivity(), mUri, PlaysQuery.PROJECTION, PlayPlayers.USER_NAME + "=?",
						new String[] { mBuddyName }, null);
					break;
			}
			if (loader != null) {
				loader.setUpdateThrottle(2000);
			}
		} else if (id == GameQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), Games.buildGameUri(mGameId), GameQuery.PROJECTION, null, null,
				null);
		}
		return loader;
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
		} else if (token == GameQuery._TOKEN) {
			if (!mAutoSyncTriggered && cursor != null && cursor.moveToFirst()) {
				mAutoSyncTriggered = true;
				long updated = cursor.getLong(GameQuery.UPDATED_PLAYS);
				if (updated == 0 || DateTimeUtils.howManyDaysOld(updated) > 2) {
					triggerRefresh();
				}
			}
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}

		mCallbacks.onPlayCountChanged(cursor.getCount());
		if (token != GameQuery._TOKEN) {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
			restoreScrollState();
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
			getLoaderManager().restartLoader(PlaysQuery._TOKEN, getArguments(), this);
		}
	}

	private class PlayAdapter extends CursorAdapter {
		private static final int STATE_UNKNOWN = 0;
		private static final int STATE_SECTIONED_CELL = 1;
		private static final int STATE_REGULAR_CELL = 2;

		SimpleDateFormat mFormatter = new SimpleDateFormat("MMMM", Locale.getDefault());
		GregorianCalendar mCalendar = new GregorianCalendar();
		private LayoutInflater mInflater;
		private int mRowResId = R.layout.row_play;
		private int[] mCellStates;
		private String mPreviousSection;
		private String mCurrentSection;

		public PlayAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
			// account for leap years
			mCalendar.set(Calendar.YEAR, 2012);
			mCalendar.set(Calendar.DAY_OF_MONTH, 1);
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
			mCurrentSection = getSection(cursor);
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
						mPreviousSection = getSection(cursor);
						if (!mPreviousSection.equals(mCurrentSection)) {
							needSeparator = true;
						}
						cursor.moveToPosition(position);
					}
					mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
					break;
			}

			if (needSeparator) {
				int month = Integer.parseInt(mCurrentSection.substring(5, 7));
				mCalendar.set(Calendar.MONTH, month - 1);
				String date = mFormatter.format(mCalendar.getTime()) + " " + mCurrentSection.substring(0, 4);
				holder.separator.setText(date);
				holder.separator.setVisibility(View.VISIBLE);
			} else {
				holder.separator.setVisibility(View.GONE);
			}

			UIUtils.setActivatedCompat(view, cursor.getInt(PlaysQuery.PLAY_ID) == mSelectedPlayId);

			holder.date.setText(cursor.getString(PlaysQuery.DATE));
			holder.name.setText(cursor.getString(PlaysQuery.GAME_NAME));
			holder.location.setText(cursor.getString(PlaysQuery.LOCATION));

			int status = cursor.getInt(PlaysQuery.SYNC_STATUS);
			if (status != Play.SYNC_STATUS_SYNCED) {
				int messageId = 0;
				if (status == Play.SYNC_STATUS_IN_PROGRESS) {
					messageId = R.string.sync_in_process;
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

		private String getSection(Cursor cursor) {
			String date = cursor.getString(PlaysQuery.DATE);
			if (TextUtils.isEmpty(date)) {
				return "1969-01";
			}
			return date.substring(0, 7);
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

	private interface GameQuery {
		int _TOKEN = 0x22;
		String[] PROJECTION = { Games.UPDATED_PLAYS };
		int UPDATED_PLAYS = 0;
	}

	private interface PlaysQuery {
		int _TOKEN = 0x21;
		String[] PROJECTION = { Plays._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, };
		int PLAY_ID = 1;
		int DATE = 2;
		int GAME_NAME = 3;
		int GAME_ID = 4;
		int LOCATION = 5;
		int SYNC_STATUS = 8;
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
					cursor.getInt(PlaysQuery.GAME_ID), cursor.getString(PlaysQuery.GAME_NAME), 0);
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
