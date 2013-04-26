package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
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

public class PlaysFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(PlaysFragment.class);
	private static final int MENU_PLAY_EDIT = Menu.FIRST;
	private static final int MENU_PLAY_DELETE = Menu.FIRST + 1;
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		listView.setCacheColorHint(Color.WHITE);
		listView.setFastScrollEnabled(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(getListView());
		setEmptyText(getString(R.string.empty_plays));
		setListShown(false);

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
		getLoaderManager().restartLoader(PlaysQuery._TOKEN, getArguments(), this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		launchPlay(cursor);
	}

	private void launchPlay(Cursor cursor) {
		int playId = cursor.getInt(PlaysQuery.PLAY_ID);
		Uri playUri = Plays.buildPlayUri(playId);
		Intent i = new Intent(Intent.ACTION_VIEW, playUri);
		i.putExtra(PlayActivity.KEY_GAME_ID, cursor.getInt(PlaysQuery.GAME_ID));
		i.putExtra(PlayActivity.KEY_GAME_NAME, cursor.getString(PlaysQuery.GAME_NAME));
		startActivity(i);
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return;
		}
		final String gameName = cursor.getString(PlaysQuery.GAME_NAME);

		menu.setHeaderTitle(gameName);
		menu.add(0, MENU_PLAY_EDIT, 0, R.string.menu_edit);
		menu.add(0, MENU_PLAY_DELETE, 0, R.string.menu_delete);
		// TODO: add Send and Share menu items
	}

	private boolean isPlayMenuItem(int itemId) {
		return itemId == MENU_PLAY_EDIT || itemId == MENU_PLAY_DELETE;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (!isPlayMenuItem(itemId)) {
			return false;
		}

		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return false;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return false;
		}

		final int playId = cursor.getInt(PlaysQuery.PLAY_ID);
		switch (itemId) {
			case MENU_PLAY_EDIT: {
				ActivityUtils.logPlay(getActivity(), playId, cursor.getInt(PlaysQuery.GAME_ID),
					cursor.getString(PlaysQuery.GAME_NAME));
				return true;
			}
			case MENU_PLAY_DELETE: {
				ActivityUtils.createConfirmationDialog(getActivity(), R.string.are_you_sure_delete_play,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							ContentValues values = new ContentValues();
							values.put(Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_DELETE);
							ContentResolver resolver = getActivity().getContentResolver();
							resolver.update(Plays.buildPlayUri(playId), values, null, null);
							SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS_UPLOAD);
						}
					}).show();
				return true;
			}
		}
		return false;
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

		if (token != GameQuery._TOKEN) {
			if (isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
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
		mFilter = filter;
		// right now, filters can't be applied while viewing by game
		if (mMode == MODE_ALL) {
			getLoaderManager().restartLoader(PlaysQuery._TOKEN, getArguments(), this);
		}
	}

	private class PlayAdapter extends CursorAdapter {
		private LayoutInflater mInflater;
		private int mRowResId = R.layout.row_play;

		public PlayAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
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
				view.setBackgroundResource(R.color.background_light);
			} else {
				holder.status.setVisibility(View.GONE);
				view.setBackgroundResource(R.color.background);
			}
		}
	}

	static class ViewHolder {
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

	private interface GameQuery {
		int _TOKEN = 0x22;
		String[] PROJECTION = { Games.UPDATED_PLAYS };
		int UPDATED_PLAYS = 0;
	}

	private interface PlaysQuery {
		int _TOKEN = 0x21;
		String[] PROJECTION = { BaseColumns._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
			Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS, };
		int PLAY_ID = 1;
		int DATE = 2;
		int GAME_NAME = 3;
		int GAME_ID = 4;
		int LOCATION = 5;
		int SYNC_STATUS = 8;
	}
}
