package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.UIUtils;

public class CollectionActivity extends TopLevelSinglePaneActivity implements LoaderManager.LoaderCallbacks<Cursor>,
	CollectionFragment.Callbacks, OnNavigationListener {
	private static final int HELP_VERSION = 1;
	private static final String STATE_VIEW_ID = "STATE_VIEW_ID";
	private static final String STATE_COUNT = "STATE_COUNT";
	private static final String STATE_SORT_NAME = "STATE_SORT_NAME";

	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;
	private boolean mShortcut;
	private CollectionViewAdapter mAdapter;
	private long mViewId;
	private int mCount;
	private String mSortName;
	private boolean mIsTitleHidden;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction());

		if (savedInstanceState != null) {
			mViewId = savedInstanceState.getLong(STATE_VIEW_ID);
			mCount = savedInstanceState.getInt(STATE_COUNT);
			mSortName = savedInstanceState.getString(STATE_SORT_NAME);
		}

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setCustomView(R.layout.actionbar_text_2line);
		if (mShortcut) {
			actionBar.setHomeButtonEnabled(false);
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setTitle(R.string.menu_create_shortcut);
		} else {
			getSupportLoaderManager().restartLoader(Query._TOKEN, null, this);
		}

		UIUtils.showHelpDialog(this, HelpUtils.HELP_COLLECTION_KEY, HELP_VERSION, R.string.help_collection);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mAdapter != null) {
			outState.putLong(STATE_VIEW_ID, mAdapter.getItemId(getSupportActionBar().getSelectedNavigationIndex()));
		}
		outState.putInt(STATE_COUNT, mCount);
		outState.putString(STATE_SORT_NAME, mSortName);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSyncStatusObserver.onStatusChanged(0);
		mSyncObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_PENDING
			| ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, mSyncStatusObserver);
	}

	@Override
	protected boolean isTitleHidden() {
		return mIsTitleHidden;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		mSyncStatusObserver.onStatusChanged(0);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ActivityUtils.setCustomActionBarText(getSupportActionBar(), R.id.menu_list_count,
			(isDrawerOpen() || mCount <= 0) ? "" : String.valueOf(mCount), isDrawerOpen() ? "" : mSortName);
		MenuItem mi = menu.findItem(R.id.menu_search);
		if (mi != null) {
			mi.setVisible(!isDrawerOpen());
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected Fragment onCreatePane() {
		return new CollectionFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return mShortcut ? 0 : R.menu.search_only;
	}

	@Override
	public boolean onGameSelected(int gameId, String gameName) {
		ActivityUtils.launchGame(this, gameId, gameName);
		return false;
	}

	@Override
	public void onSetShortcut(Intent intent) {
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onCollectionCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onSortChanged(String sortName) {
		mSortName = sortName;
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onViewRequested(long viewId) {
		mViewId = viewId;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			loader = new CursorLoader(this, CollectionViews.CONTENT_URI, Query.PROJECTION, null, null, null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (loader.getId() == Query._TOKEN) {
			if (mAdapter == null) {
				mAdapter = new CollectionViewAdapter(this, cursor);
			} else {
				mAdapter.changeCursor(cursor);
			}
			final ActionBar actionBar = getSupportActionBar();
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setListNavigationCallbacks(mAdapter, this);
			actionBar.setSelectedNavigationItem(findViewIndex(mViewId));
			mIsTitleHidden = true;
		} else {
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		CollectionFragment fragment = (CollectionFragment) getFragment();
		if (itemId < 0) {
			fragment.clearView();
		} else {
			fragment.setView(itemId);
		}
		return true;
	}

	private int findViewIndex(long viewId) {
		int index = 0;
		if (viewId > 0) {
			Cursor c = mAdapter.getCursor();
			if (c != null && c.moveToFirst()) {
				do {
					if (viewId == c.getLong(Query._ID)) {
						return c.getPosition() + 1;
					}
				} while (c.moveToNext());
			}
		}
		return index;
	}

	private void setRefreshActionButtonState(boolean refreshing) {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
			} else {
				refreshItem.setActionView(null);
			}
		}
	}

	private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
		@Override
		public void onStatusChanged(int which) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setRefreshActionButtonState(SyncService.isActiveOrPending(CollectionActivity.this));
				}
			});
		}
	};

	private static class CollectionViewAdapter extends SimpleCursorAdapter {
		private LayoutInflater mInflater;

		public CollectionViewAdapter(Context context, Cursor cursor) {
			super(context, R.layout.sherlock_spinner_item, cursor, new String[] { CollectionViews._ID,
				CollectionViews.NAME }, new int[] { 0, android.R.id.text1 }, 0);
			setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return super.getCount() + 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				return createDefaultItem(convertView, parent, R.layout.sherlock_spinner_item);
			} else {
				return super.getView(position - 1, convertView, parent);
			}
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				return createDefaultItem(convertView, parent, R.layout.sherlock_spinner_dropdown_item);
			} else {
				return super.getDropDownView(position - 1, convertView, parent);
			}
		}

		private View createDefaultItem(View convertView, ViewGroup parent, int layout) {
			View v;
			if (convertView == null) {
				v = mInflater.inflate(layout, parent, false);
			} else {
				v = convertView;
			}
			((TextView) v).setText(R.string.title_collection);
			return v;
		}

		@Override
		public Object getItem(int position) {
			if (position == 0) {
				return null;
			}
			return super.getItem(position - 1);
		}

		@Override
		public long getItemId(int position) {
			if (position == 0) {
				return -1;
			}
			return super.getItemId(position - 1);
		}
	}

	private interface Query {
		int _TOKEN = 0x01;
		String[] PROJECTION = { CollectionViews._ID, CollectionViews.NAME, CollectionViews.SORT_TYPE, };
		int _ID = 0;
	}
}