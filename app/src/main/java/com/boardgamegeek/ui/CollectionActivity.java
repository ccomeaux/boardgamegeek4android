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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionViewRequestedEvent;
import com.boardgamegeek.events.GameSelectedEvent;
import com.boardgamegeek.events.GameShortcutCreatedEvent;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;

import org.greenrobot.eventbus.Subscribe;

import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class CollectionActivity extends TopLevelSinglePaneActivity implements LoaderCallbacks<Cursor> {
	private CollectionViewAdapter adapter;
	private long viewId;
	@State int viewIndex;
	private Spinner spinner;
	private boolean isCreatingShortcut;

	@Override
	@DebugLog
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		viewId = savedInstanceState != null ? -1 : PreferencesUtils.getViewDefaultId(this);

		isCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction());
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (isCreatingShortcut) {
				actionBar.setHomeButtonEnabled(false);
				actionBar.setDisplayHomeAsUpEnabled(false);
				actionBar.setTitle(R.string.app_name);
			} else {
				actionBar.setDisplayShowTitleEnabled(false);
				actionBar.setDisplayShowCustomEnabled(true);
				actionBar.setCustomView(R.layout.actionbar_collection);
			}
		}
		if (!isCreatingShortcut) {
			getSupportLoaderManager().restartLoader(Query._TOKEN, null, this);
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Collection"));
		}
	}

	@Override
	@DebugLog
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		spinner = (Spinner) findViewById(R.id.menu_spinner);
		bindSpinner();
		return true;
	}

	@Override
	protected int getOptionsMenuId() {
		if (isCreatingShortcut) {
			return super.getOptionsMenuId();
		} else {
			return R.menu.search;
		}
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
			bindSpinner();
		} else {
			cursor.close();
		}
	}

	private void bindSpinner() {
		if (spinner != null && adapter != null) {
			spinner.setAdapter(adapter);
			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					CollectionFragment fragment = (CollectionFragment) getFragment();
					long oldId = fragment.getViewId();
					if (id != oldId) {
						Answers.getInstance().logCustom(new CustomEvent("CollectionViewSelected"));
						viewIndex = findViewIndex(id);
						if (id < 0) {
							fragment.clearView();
						} else {
							fragment.setView(id);
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// Do nothing
				}
			});
			spinner.setSelection(viewIndex);
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.changeCursor(null);
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
			super(context,
				R.layout.actionbar_spinner_item,
				cursor,
				new String[] { CollectionViews._ID, CollectionViews.NAME },
				new int[] { 0, android.R.id.text1 },
				0);
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
				return createDefaultItem(convertView, parent, R.layout.actionbar_spinner_item);
			} else {
				try {
					return super.getView(position - 1, convertView, parent);
				} catch (IllegalStateException e) {
					return createDefaultItem(convertView, parent, R.layout.actionbar_spinner_item);
				}
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
