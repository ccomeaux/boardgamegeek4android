package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionViewRequestedEvent;
import com.boardgamegeek.events.GameSelectedEvent;
import com.boardgamegeek.events.GameShortcutCreatedEvent;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShortcutUtils;

import org.greenrobot.eventbus.Subscribe;

import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class CollectionActivity extends TopLevelSinglePaneActivity implements LoaderCallbacks<Cursor>, OnNavigationListener {
	private CollectionViewAdapter adapter;
	private long viewId;
	private boolean isTitleHidden;
	@State int viewIndex;

	@Override
	@DebugLog
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		viewId = savedInstanceState != null ? -1 : PreferencesUtils.getViewDefaultId(this);

		boolean shortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction());
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
			actionBar.setCustomView(R.layout.actionbar_text_2line);
			if (shortcut) {
				actionBar.setHomeButtonEnabled(false);
				actionBar.setDisplayHomeAsUpEnabled(false);
				actionBar.setTitle(R.string.app_name);
			}
		}
		if (!shortcut) {
			getSupportLoaderManager().restartLoader(Query._TOKEN, null, this);
		}
	}

	@Override
	@DebugLog
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_search) {
			Intent intent = new Intent(this, SearchResultsActivity.class);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected boolean isTitleHidden() {
		return isTitleHidden;
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
	@Subscribe
	public void onEvent(GameSelectedEvent event) {
		ActivityUtils.launchGame(this, event.getId(), event.getName());
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(GameShortcutCreatedEvent event) {
		Intent intent = ShortcutUtils.createIntent(this, event.getId(), event.getName(), event.getThumbnailUrl());
		setResult(RESULT_OK, intent);
		finish();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(CollectionViewRequestedEvent event) {
		viewId = event.getViewId();
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
		private final LayoutInflater inflater;

		public CollectionViewAdapter(Context context, Cursor cursor) {
			super(context, android.R.layout.simple_spinner_item, cursor, new String[] { CollectionViews._ID,
				CollectionViews.NAME }, new int[] { 0, android.R.id.text1 }, 0);
			setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
				v = inflater.inflate(layout, parent, false);
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