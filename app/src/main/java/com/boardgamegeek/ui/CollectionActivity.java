package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.GameShortcutRequestedEvent;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.tasks.SelectCollectionViewTask;
import com.boardgamegeek.ui.adapter.CollectionViewAdapter;
import com.boardgamegeek.ui.dialog.CollectionFilterDialogFragment;
import com.boardgamegeek.ui.dialog.CollectionSortDialogFragment;
import com.boardgamegeek.ui.dialog.DeleteViewDialogFragment;
import com.boardgamegeek.ui.dialog.SaveViewDialogFragment;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.fabric.CollectionViewManipulationEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class CollectionActivity extends TopLevelSinglePaneActivity implements
	LoaderCallbacks<Cursor>,
	SaveViewDialogFragment.OnViewSavedListener,
	DeleteViewDialogFragment.OnViewDeletedListener,
	CollectionSortDialogFragment.Listener,
	CollectionFilterDialogFragment.Listener {
	private static final String KEY_VIEW_ID = "VIEW_ID";
	private CollectionViewAdapter adapter;
	private long viewId;
	@State int viewIndex;
	private Spinner spinner;
	private boolean isCreatingShortcut;

	public static Intent createIntentAsShortcut(Context context, long viewId) {
		return new Intent(context, CollectionActivity.class)
			.setAction(Intent.ACTION_VIEW)
			.putExtra(KEY_VIEW_ID, viewId)
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	}

	@Override
	@DebugLog
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		viewId = getIntent().getLongExtra(KEY_VIEW_ID, savedInstanceState != null ? -1 : PreferencesUtils.getViewDefaultId(this));

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
			LoaderManager.getInstance(this).restartLoader(Query._TOKEN, null, this);
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Collection"));
		}
	}

	@Override
	protected void readIntent(@NonNull Intent intent) {
		isCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction());
	}

	@Override
	@DebugLog
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		super.onCreateOptionsMenu(menu);
		spinner = findViewById(R.id.menu_spinner);
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
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.menu_search) {
			Intent intent = new Intent(this, SearchResultsActivity.class);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@NonNull
	@Override
	@DebugLog
	protected Fragment onCreatePane() {
		return CollectionFragment.newInstance(isCreatingShortcut);
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_collection;
	}

	@Override
	public void onInsertRequested(String name, boolean isDefault) {
		long viewId = ((CollectionFragment) getFragment()).insertView(name);
		setOrRemoveDefault(viewId, isDefault);
		notifyViewCreated(viewId, name);
	}

	@Override
	public void onUpdateRequested(String name, boolean isDefault, long viewId) {
		((CollectionFragment) getFragment()).updateView(viewId);
		setOrRemoveDefault(viewId, isDefault);
		notifyViewCreated(viewId, name);
	}

	@Override
	public void onViewDeleted(long viewId) {
		CollectionViewManipulationEvent.log("Delete");
		Toast.makeText(this, R.string.msg_collection_view_deleted, Toast.LENGTH_SHORT).show();
		if (viewId == this.viewId) {
			viewIndex = findViewIndex(PreferencesUtils.getViewDefaultId(this));
		}
	}

	private void setOrRemoveDefault(long viewId, boolean isDefault) {
		if (isDefault) {
			// TODO: prompt the user if replacing a default
			PreferencesUtils.putViewDefaultId(this, viewId);
		} else {
			if (viewId == PreferencesUtils.getViewDefaultId(this)) {
				PreferencesUtils.removeViewDefaultId(this);
			}
		}
	}

	public void notifyViewCreated(long id, String name) {
		CollectionViewManipulationEvent.log("Create", name);
		Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
		viewIndex = findViewIndex(id);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(@NonNull GameShortcutRequestedEvent event) {
		Intent shortcutIntent = GameActivity.createIntentAsShortcut(this, event.getId(), event.getName(), event.getThumbnailUrl());
		if (shortcutIntent != null) {
			Intent intent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				intent = createShortcutForOreo(event, shortcutIntent);
			} else {
				intent = ShortcutUtils.createShortcutIntent(this, event.getName(), shortcutIntent);
				File file = ShortcutUtils.getThumbnailFile(this, event.getThumbnailUrl());
				if (file != null && file.exists()) {
					intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapFactory.decodeFile(file.getAbsolutePath()));
				}
			}
			if (intent != null) setResult(RESULT_OK, intent);
		}
		finish();
	}

	@RequiresApi(api = VERSION_CODES.O)
	@Nullable
	private Intent createShortcutForOreo(@NonNull GameShortcutRequestedEvent event, @NonNull Intent shortcutIntent) {
		ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
		if (shortcutManager == null) return null;
		ShortcutInfo.Builder builder = new ShortcutInfo.Builder(this, ShortcutUtils.createGameShortcutId(event.getId()))
			.setShortLabel(StringUtils.limitText(event.getName(), ShortcutUtils.SHORT_LABEL_LENGTH))
			.setLongLabel(StringUtils.limitText(event.getName(), ShortcutUtils.LONG_LABEL_LENGTH))
			.setIntent(shortcutIntent);
		File file = ShortcutUtils.getThumbnailFile(this, event.getThumbnailUrl());
		if (file != null && file.exists()) {
			builder.setIcon(Icon.createWithAdaptiveBitmap(BitmapFactory.decodeFile(file.getAbsolutePath())));
		} else {
			builder.setIcon(Icon.createWithResource(this, R.drawable.ic_adaptive_game));
		}
		return shortcutManager.createShortcutResultIntent(builder.build());
	}

	@Nullable
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
	public void onLoadFinished(@NonNull Loader<Cursor> loader, @NonNull Cursor cursor) {
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
					long oldId = fragment != null ? fragment.getViewId() : id;
					if (id != oldId) {
						Answers.getInstance().logCustom(new CustomEvent("CollectionViewSelected"));
						viewId = id;
						viewIndex = findViewIndex(id);
						if (id < 0) {
							fragment.clearView();
						} else {
							fragment.setView(id);
							TaskUtils.executeAsyncTask(new SelectCollectionViewTask(CollectionActivity.this, id));
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

	@Override
	public void onSortSelected(int sortType) {
		((CollectionFragment) getFragment()).setSort(sortType);
	}

	@Override
	public void onFilterSelected(int filterType) {
		((CollectionFragment) getFragment()).launchFilterDialog(filterType);
	}

	private interface Query {
		int _TOKEN = 0x01;
		String[] PROJECTION = { CollectionViews._ID, CollectionViews.NAME, CollectionViews.SORT_TYPE, };
		int _ID = 0;
	}
}
