package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
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
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.adapter.CollectionViewAdapter;
import com.boardgamegeek.ui.dialog.CollectionFilterDialogFragment;
import com.boardgamegeek.ui.dialog.CollectionSortDialogFragment;
import com.boardgamegeek.ui.dialog.DeleteViewDialogFragment;
import com.boardgamegeek.ui.dialog.SaveViewDialogFragment;
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.fabric.CollectionViewManipulationEvent;
import com.boardgamegeek.util.fabric.SortEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;

import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import hugo.weaving.DebugLog;

public class CollectionActivity extends TopLevelSinglePaneActivity implements
	SaveViewDialogFragment.OnViewSavedListener,
	DeleteViewDialogFragment.OnViewDeletedListener,
	CollectionSortDialogFragment.Listener,
	CollectionFilterDialogFragment.Listener {
	private static final String KEY_VIEW_ID = "VIEW_ID";
	private static final String KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID";
	private CollectionViewAdapter adapter;
	private boolean isCreatingShortcut;
	private long changingGamePlayId;
	private CollectionViewViewModel viewModel;

	public static Intent createIntentAsShortcut(Context context, long viewId) {
		return new Intent(context, CollectionActivity.class)
			.setAction(Intent.ACTION_VIEW)
			.putExtra(KEY_VIEW_ID, viewId)
			.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	}

	public static Intent createIntentForGameChange(Context context, long playId) {
		return new Intent(context, CollectionActivity.class)
			.putExtra(KEY_CHANGING_GAME_PLAY_ID, playId);
	}

	@Override
	@DebugLog
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID) {
				actionBar.setHomeButtonEnabled(false);
				actionBar.setDisplayHomeAsUpEnabled(false);
				actionBar.setTitle(R.string.app_name);
			} else {
				actionBar.setDisplayShowTitleEnabled(false);
				actionBar.setDisplayShowCustomEnabled(true);
				actionBar.setCustomView(R.layout.actionbar_collection);
			}
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Collection"));
		}

		long viewId = getIntent().getLongExtra(KEY_VIEW_ID, savedInstanceState != null ? -1 : PreferencesUtils.getViewDefaultId(this));
		viewModel = ViewModelProviders.of(this).get(CollectionViewViewModel.class);
		viewModel.selectView(viewId);
	}

	@Override
	protected void readIntent(@NonNull Intent intent) {
		isCreatingShortcut = Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction());
		changingGamePlayId = getIntent().getLongExtra(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID);
	}

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		super.onCreateOptionsMenu(menu);
		Spinner spinner = findViewById(R.id.menu_spinner);
		if (spinner != null) {
			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					Answers.getInstance().logCustom(new CustomEvent("CollectionViewSelected"));
					if (id < 0) {
						viewModel.clearView();
					} else {
						viewModel.selectView(id);
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// Do nothing
				}
			});

			viewModel.getViews().observe(this, collectionViews -> {
				if (adapter == null) {
					adapter = new CollectionViewAdapter(CollectionActivity.this, collectionViews);
					spinner.setAdapter(adapter);
				} else if (!collectionViews.isEmpty()) {
					// TODO sometimes on pull-to-refresh, the collectionViews is an empty list
					adapter.clear();
					adapter.addAll(collectionViews);
				}
				Long viewId = viewModel.getSelectedViewId().getValue();
				int index = adapter.findIndexOf(viewId);
				spinner.setSelection(index);
			});
		}

		return true;
	}

	@Override
	protected int getOptionsMenuId() {
		if (isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID) {
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
		if (changingGamePlayId != BggContract.INVALID_ID) {
			return CollectionFragment.newInstanceForPlayGameChange(changingGamePlayId);
		} else {
			return CollectionFragment.newInstance(isCreatingShortcut);
		}
	}

	@Override
	protected int getNavigationItemId() {
		return R.id.collection;
	}

	@Override
	public void onInsertRequested(@NotNull String name, boolean isDefault) {
		long viewId = viewModel.insert(name);
		setOrRemoveDefault(viewId, isDefault);
		notifyViewCreated(name);
	}

	@Override
	public void onUpdateRequested(@NotNull String name, boolean isDefault, long viewId) {
		viewModel.update();
		setOrRemoveDefault(viewId, isDefault);
		notifyViewCreated(name);
	}

	@Override
	public void onViewDeleted(long viewId) {
		CollectionViewManipulationEvent.log("Delete");
		Toast.makeText(this, R.string.msg_collection_view_deleted, Toast.LENGTH_SHORT).show();
		if (viewId == viewModel.getSelectedViewId().getValue()) {
			// TODO: create selectDefaultView method
			viewModel.selectView(PreferencesUtils.getViewDefaultId(this));
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

	public void notifyViewCreated(String name) {
		CollectionViewManipulationEvent.log("Create", name);
		Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
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


	@Override
	public void onSortSelected(int sortType) {
		viewModel.setSort(sortType);
		SortEvent.log("Collection", String.valueOf(sortType));
	}

	@Override
	public void onFilterSelected(int filterType) {
		((CollectionFragment) getFragment()).launchFilterDialog(filterType);
	}
}
