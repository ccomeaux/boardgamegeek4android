package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionCountChangedEvent;
import com.boardgamegeek.events.CollectionSortChangedEvent;
import com.boardgamegeek.events.CollectionViewRequestedEvent;
import com.boardgamegeek.events.GameSelectedEvent;
import com.boardgamegeek.events.GameShortcutCreatedEvent;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class CollectionActivity extends TopLevelSinglePaneActivity implements LoaderManager.LoaderCallbacks<Cursor>, OnNavigationListener {
	private static final int HELP_VERSION = 1;
	private static final String STATE_VIEW_INDEX = "STATE_VIEW_INDEX";
	private static final String STATE_COUNT = "STATE_COUNT";
	private static final String STATE_SORT_NAME = "STATE_SORT_NAME";

	private CollectionViewAdapter adapter;
	private long viewId;
	private int rowCount;
	private String sortName;
	private boolean isTitleHidden;
	private int viewIndex;

	@Override
	@DebugLog
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			viewId = -1;
			viewIndex = savedInstanceState.getInt(STATE_VIEW_INDEX);
			rowCount = savedInstanceState.getInt(STATE_COUNT);
			sortName = savedInstanceState.getString(STATE_SORT_NAME);
		} else {
			viewId = PreferencesUtils.getViewDefaultId(this);
		}

		boolean shortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction());
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
			actionBar.setCustomView(R.layout.actionbar_text_2line);
			if (shortcut) {
				actionBar.setHomeButtonEnabled(false);
				actionBar.setDisplayHomeAsUpEnabled(false);
				actionBar.setTitle(R.string.menu_create_shortcut);
			}
		}
		if (!shortcut) {
			getSupportLoaderManager().restartLoader(Query._TOKEN, null, this);
		}

		HelpUtils.showHelpDialog(this, HelpUtils.HELP_COLLECTION_KEY, HELP_VERSION, R.string.help_collection);
	}

	@Override
	@DebugLog
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_VIEW_INDEX, viewIndex);
		outState.putInt(STATE_COUNT, rowCount);
		outState.putString(STATE_SORT_NAME, sortName);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected boolean isTitleHidden() {
		return isTitleHidden;
	}

	@Override
	@DebugLog
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean hide = (isDrawerOpen() || rowCount <= 0);
		ToolbarUtils.setCustomActionBarText(getSupportActionBar(),
			hide ? "" : String.valueOf(rowCount),
			hide ? "" : sortName);
		MenuItem mi = menu.findItem(R.id.menu_search);
		if (mi != null) {
			mi.setVisible(!isDrawerOpen());
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	@DebugLog
	protected Fragment onCreatePane() {
		return new CollectionFragment();
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_collection;
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(GameSelectedEvent event) {
		ActivityUtils.launchGame(this, event.id, event.name);
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(GameShortcutCreatedEvent event) {
		Intent intent = ShortcutUtils.createIntent(this, event.id, event.name, event.thumbnailUrl);
		setResult(RESULT_OK, intent);
		finish();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(CollectionCountChangedEvent event) {
		rowCount = event.count;
		supportInvalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(CollectionSortChangedEvent event) {
		sortName = event.name;
		supportInvalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(CollectionViewRequestedEvent event) {
		viewId = event.viewId;
		viewIndex = findViewIndex(viewId);
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			loader = new CursorLoader(this, CollectionViews.CONTENT_URI, Query.PROJECTION, null, null, null);
		}
		return loader;
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (loader.getId() == Query._TOKEN) {
			if (adapter == null) {
				adapter = new CollectionViewAdapter(this, cursor);
			} else {
				adapter.changeCursor(cursor);
			}
			if (viewId != -1) {
				viewIndex = findViewIndex(viewId);
			}
			final ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setDisplayShowTitleEnabled(false);
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
				actionBar.setListNavigationCallbacks(adapter, this);
				actionBar.setSelectedNavigationItem(viewIndex);
			}
			isTitleHidden = true;
		} else {
			cursor.close();
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.changeCursor(null);
	}

	@Override
	@DebugLog
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		CollectionFragment fragment = (CollectionFragment) getFragment();
		long oldId = fragment.getViewId();
		if (itemId != oldId) {
			viewIndex = findViewIndex(itemId);
			if (itemId < 0) {
				fragment.clearView();
			} else {
				fragment.setView(itemId);
			}
		}
		return true;
	}

	@DebugLog
	private int findViewIndex(long viewId) {
		int index = 0;
		if (viewId > 0) {
			Cursor c = adapter.getCursor();
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

	private static class CollectionViewAdapter extends SimpleCursorAdapter {
		private final LayoutInflater mInflater;

		public CollectionViewAdapter(Context context, Cursor cursor) {
			super(context, android.R.layout.simple_spinner_item, cursor, new String[] { CollectionViews._ID,
				CollectionViews.NAME }, new int[] { 0, android.R.id.text1 }, 0);
			setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return super.getCount() + 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				return createDefaultItem(convertView, parent, android.R.layout.simple_spinner_item);
			} else {
				return super.getView(position - 1, convertView, parent);
			}
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				return createDefaultItem(convertView, parent, R.layout.support_simple_spinner_dropdown_item);
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
				return PreferencesUtils.VIEW_ID_COLLECTION;
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