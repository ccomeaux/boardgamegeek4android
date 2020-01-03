package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.entities.ConstantsKt;
import com.boardgamegeek.events.CollectionCountChangedEvent;
import com.boardgamegeek.events.CollectionSortChangedEvent;
import com.boardgamegeek.events.GameShortcutRequestedEvent;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.extensions.IntUtils;
import com.boardgamegeek.extensions.PreferenceUtils;
import com.boardgamegeek.extensions.SwipeRefreshLayoutUtils;
import com.boardgamegeek.extensions.TextViewUtils;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.CollectionFiltererFactory;
import com.boardgamegeek.filterer.CollectionStatusFilterer;
import com.boardgamegeek.pref.SettingsActivity;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.sorter.CollectionSorter;
import com.boardgamegeek.sorter.CollectionSorterFactory;
import com.boardgamegeek.sorter.Sorter;
import com.boardgamegeek.ui.dialog.CollectionFilterDialog;
import com.boardgamegeek.ui.dialog.CollectionFilterDialogFactory;
import com.boardgamegeek.ui.dialog.CollectionFilterDialogFragment;
import com.boardgamegeek.ui.dialog.CollectionSortDialogFragment;
import com.boardgamegeek.ui.dialog.DeleteViewDialogFragment;
import com.boardgamegeek.ui.dialog.SaveViewDialogFragment;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.ui.widget.ToolbarActionItemTarget;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.ShowcaseViewWizard;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.fabric.FilterEvent;
import com.boardgamegeek.util.fabric.SortEvent;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.github.amlcurran.showcaseview.targets.Target;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import timber.log.Timber;

public class CollectionFragment extends Fragment implements
	LoaderCallbacks<Cursor>,
	OnRefreshListener,
	ActionMode.Callback,
	CollectionFilterDialog.OnFilterChangedListener {
	private static final String KEY_IS_CREATING_SHORTCUT = "IS_CREATING_SHORTCUT";
	private static final String KEY_CHANGING_GAME_PLAY_ID = "KEY_CHANGING_GAME_PLAY_ID";
	private static final int HELP_VERSION = 2;

	private Unbinder unbinder;
	@BindView(R.id.empty_container) ViewGroup emptyContainer;
	@BindView(android.R.id.empty) TextView emptyTextView;
	@BindView(R.id.empty_button) Button emptyButton;
	@BindView(R.id.progress) ContentLoadingProgressBar progressBar;
	@BindView(android.R.id.list) RecyclerView listView;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.toolbar_footer) Toolbar footerToolbar;
	@BindView(R.id.row_count) TextView rowCountView;
	@BindView(R.id.sort_description) TextView sortDescriptionView;
	@BindView(R.id.chipGroupScrollView) HorizontalScrollView chipGroupScrollView;
	@BindView(R.id.chipGroup) ChipGroup chipGroup;

	private CollectionAdapter adapter;
	@State long viewId;
	@State @Nullable String viewName = "";
	@State int sortType;
	@State ArrayList<Integer> types;
	@State ArrayList<String> data;
	private CollectionSorter sorter;
	private final List<CollectionFilterer> filters = new ArrayList<>();
	private String defaultWhereClause;
	private boolean isCreatingShortcut;
	private long changingGamePlayId;
	private boolean isSyncing;
	private ActionMode actionMode = null;
	private CollectionSorterFactory collectionSorterFactory;
	private ShowcaseViewWizard showcaseViewWizard;

	public static CollectionFragment newInstance(boolean isCreatingShortcut) {
		Bundle args = new Bundle();
		args.putBoolean(KEY_IS_CREATING_SHORTCUT, isCreatingShortcut);
		CollectionFragment fragment = new CollectionFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static CollectionFragment newInstanceForPlayGameChange(long playId) {
		Bundle args = new Bundle();
		args.putLong(KEY_CHANGING_GAME_PLAY_ID, playId);
		CollectionFragment fragment = new CollectionFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	@DebugLog
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
		setHasOptionsMenu(true);
		if (savedInstanceState != null) {
			Icepick.restoreInstanceState(this, savedInstanceState);

			filters.clear();
			if (types != null && data != null) {
				if (types.size() != data.size()) {
					Timber.w("Mismatched size of arrays: types.size() = %1$s; data.size() = %2$s", types.size(), data.size());
				} else {
					CollectionFiltererFactory factory = new CollectionFiltererFactory(getActivity());
					for (int i = 0; i < types.size(); i++) {
						final Integer filterType = types.get(i);
						CollectionFilterer filterer = factory.create(filterType);
						if (filterer == null) {
							Timber.w("Couldn't create filterer with type %s", filterType);
						} else {
							filterer.inflate(data.get(i));
							filters.add(filterer);
						}
					}
				}
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		isCreatingShortcut = bundle.getBoolean(KEY_IS_CREATING_SHORTCUT);
		changingGamePlayId = bundle.getLong(KEY_CHANGING_GAME_PLAY_ID, BggContract.INVALID_ID);
	}

	@Override
	@DebugLog
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_collection, container, false);
		unbinder = ButterKnife.bind(this, view);
		return view;
	}

	@Override
	@DebugLog
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (isCreatingShortcut) {
			Snackbar.make(swipeRefreshLayout, R.string.msg_shortcut_create, Snackbar.LENGTH_LONG).show();
		} else if (changingGamePlayId != BggContract.INVALID_ID) {
			Snackbar.make(swipeRefreshLayout, R.string.msg_change_play_game, Snackbar.LENGTH_LONG).show();
		}

		footerToolbar.inflateMenu(R.menu.collection_fragment);
		footerToolbar.setOnMenuItemClickListener(footerMenuListener);
		invalidateMenu();

		setEmptyText();

		SwipeRefreshLayoutUtils.setBggColors(swipeRefreshLayout);
		swipeRefreshLayout.setOnRefreshListener(this);

		createShowcaseViewWizard();
		showcaseViewWizard.maybeShowHelp();
	}

	private void createShowcaseViewWizard() {
		showcaseViewWizard = new ShowcaseViewWizard(getActivity(), HelpUtils.HELP_COLLECTION_KEY, HELP_VERSION);
		showcaseViewWizard.addTarget(R.string.help_collection, Target.NONE);
		showcaseViewWizard.addTarget(R.string.help_collection_sort, new ToolbarActionItemTarget(R.id.menu_collection_sort, footerToolbar));
		showcaseViewWizard.addTarget(R.string.help_collection_filter, new ToolbarActionItemTarget(R.id.menu_collection_filter, footerToolbar));
		showcaseViewWizard.addTarget(R.string.help_collection_save, new ToolbarActionItemTarget(R.id.menu_collection_view_save, footerToolbar));
		showcaseViewWizard.addTarget(R.string.help_collection_random, new ToolbarActionItemTarget(R.id.menu_collection_random_game, footerToolbar));
		showcaseViewWizard.addTarget(R.string.help_collection_long_click, Target.NONE);
	}

	@Override
	@DebugLog
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Icepick.restoreInstanceState(this, savedInstanceState);
		sorter = getCollectionSorter(sortType);
		if (savedInstanceState != null || isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID) {
			requery();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	private CollectionSorter getCollectionSorter(int sortType) {
		if (collectionSorterFactory == null) {
			collectionSorterFactory = new CollectionSorterFactory(getActivity());
		}
		return collectionSorterFactory.create(sortType);
	}

	@DebugLog
	private void requery() {
		progressBar.show();
		LoaderManager.getInstance(this).restartLoader(Query._TOKEN, null, this);
	}

	@Override
	@DebugLog
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		sortType = sorter == null ? CollectionSorterFactory.TYPE_UNKNOWN : sorter.getType();
		types = new ArrayList<>();
		data = new ArrayList<>();
		for (CollectionFilterer filterer : filters) {
			types.add(filterer.getType());
			data.add(filterer.deflate());
		}
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.help, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_help) {
			showcaseViewWizard.showHelp();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(@NonNull SyncEvent event) {
		if ((event.getType() & SyncService.FLAG_SYNC_COLLECTION) == SyncService.FLAG_SYNC_COLLECTION) {
			isSyncing(true);
		}
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(SyncCompleteEvent event) {
		isSyncing(false);
	}

	protected void isSyncing(boolean value) {
		isSyncing = value;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(() -> {
				if (swipeRefreshLayout != null) {
					swipeRefreshLayout.setRefreshing(isSyncing);
				}
			});
		}
	}

	private void invalidateMenu() {
		final Menu menu = footerToolbar.getMenu();
		if (isCreatingShortcut || changingGamePlayId != BggContract.INVALID_ID) {
			menu.findItem(R.id.menu_collection_random_game).setVisible(false);
			menu.findItem(R.id.menu_create_shortcut).setVisible(false);
			menu.findItem(R.id.menu_collection_view_save).setVisible(false);
			menu.findItem(R.id.menu_collection_view_delete).setVisible(false);
			menu.findItem(R.id.menu_share).setVisible(false);
		} else {
			menu.findItem(R.id.menu_collection_random_game).setVisible(true);
			menu.findItem(R.id.menu_create_shortcut).setVisible(true);
			menu.findItem(R.id.menu_collection_view_save).setVisible(true);
			menu.findItem(R.id.menu_collection_view_delete).setVisible(true);
			menu.findItem(R.id.menu_share).setVisible(true);

			final boolean hasFiltersApplied = (filters.size() > 0);
			final boolean hasSortApplied = sorter != null && sorter.getType() != CollectionSorterFactory.TYPE_DEFAULT;
			final boolean hasViews = getActivity() != null && ResolverUtils.getCount(getActivity().getContentResolver(), CollectionViews.CONTENT_URI) > 0;
			final boolean hasItems = adapter != null && adapter.getItemCount() > 0;
			final boolean hasViewSelected = viewId > 0;

			menu.findItem(R.id.menu_collection_view_save).setEnabled(hasFiltersApplied || hasSortApplied);
			menu.findItem(R.id.menu_collection_view_delete).setEnabled(hasViews);
			menu.findItem(R.id.menu_collection_random_game).setEnabled(hasItems);
			menu.findItem(R.id.menu_create_shortcut).setEnabled(hasViewSelected);
			menu.findItem(R.id.menu_share).setEnabled(hasItems);
		}
	}

	final OnMenuItemClickListener footerMenuListener = new OnMenuItemClickListener() {
		@DebugLog
		@Override
		public boolean onMenuItemClick(@NonNull MenuItem item) {
			switch (item.getItemId()) {
				case R.id.menu_collection_random_game:
					Answers.getInstance().logCustom(new CustomEvent("RandomGame"));
					final CollectionItem ci = adapter.getRandomItem();
					if (ci != null) {
						GameActivity.start(requireContext(),
							ci.gameId,
							ci.gameName,
							ci.thumbnailUrl,
							ci.heroImageUrl
						);
					}
					return true;
				case R.id.menu_create_shortcut:
					if (viewId > 0) {
						ShortcutUtils.createCollectionShortcut(getContext(), viewId, viewName);
						return true;
					}
					break;
				case R.id.menu_collection_view_save:
					SaveViewDialogFragment dialog = SaveViewDialogFragment.newInstance(viewName, createViewDescription(sorter, filters));
					DialogUtils.showAndSurvive(CollectionFragment.this, dialog);
					return true;
				case R.id.menu_collection_view_delete:
					DeleteViewDialogFragment ddf = DeleteViewDialogFragment.newInstance();
					DialogUtils.showAndSurvive(CollectionFragment.this, ddf);
					return true;
				case R.id.menu_share:
					shareCollection();
					return true;
				case R.id.menu_collection_sort:
					DialogUtils.showAndSurvive(CollectionFragment.this, CollectionSortDialogFragment.newInstance(sorter.getType()));
					return true;
				case R.id.menu_collection_filter:
					ArrayList<Integer> filterTypes = new ArrayList<>();
					for (CollectionFilterer filter : filters) {
						filterTypes.add(filter.getType());
					}
					DialogUtils.showAndSurvive(CollectionFragment.this, CollectionFilterDialogFragment.newInstance(filterTypes));
					return true;
			}
			return launchFilterDialog(item.getItemId());
		}
	};

	private void shareCollection() {
		final String username = AccountUtils.getUsername(getContext());
		String description;
		if (viewId > 0 && !TextUtils.isEmpty(viewName)) {
			description = viewName;
		} else if (filters.size() > 0) {
			description = getString(R.string.title_filtered_collection);
		} else {
			description = getString(R.string.title_collection);
		}

		StringBuilder text = new StringBuilder(description)
			.append("\n")
			.append(StringUtils.repeat("-", description.length()))
			.append("\n");

		int gameCount = 0;
		final int MAX_GAMES = 10;
		for (int i = 0; i < MAX_GAMES; i++) {
			CollectionItem item = adapter.getItem(i);
			if (item == null) break;
			gameCount++;
			text.append("\u2022 ").append(ActivityUtils.formatGameLink(item.gameId, item.collectionName));
			if (gameCount >= MAX_GAMES) {
				int leftOverCount = adapter.getItemCount() - MAX_GAMES;
				if (leftOverCount > 0)
					text.append(getString(R.string.and_more, leftOverCount)).append("\n");
				break;
			}
		}

		text.append("\n")
			.append(createViewDescription(sorter, filters))
			.append("\n")
			.append("\n")
			.append(getString(R.string.share_collection_complete_footer, "https://www.boardgamegeek.com/collection/user/" + HttpUtils.encode(username)));

		ActivityUtils.share(getActivity(),
			getString(R.string.share_collection_subject, AccountUtils.getFullName(getContext()), username),
			text,
			R.string.title_share_collection);
	}

	@Override
	public void onRefresh() {
		SyncService.sync(getActivity(), SyncService.FLAG_SYNC_COLLECTION);
	}

	@NonNull
	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			StringBuilder where = new StringBuilder();
			String[] args = {};
			StringBuilder having = new StringBuilder();
			if (viewId == 0 && filters.size() == 0) {
				where.append(buildDefaultWhereClause());
			} else {
				for (CollectionFilterer filter : filters) {
					if (filter != null) {
						if (!TextUtils.isEmpty(filter.getSelection())) {
							if (where.length() > 0) where.append(" AND ");
							where.append("(").append(filter.getSelection()).append(")");
							args = StringUtils.concatenate(args, filter.getSelectionArgs());
						}
						if (!TextUtils.isEmpty(filter.getHaving())) {
							if (having.length() > 0) having.append(" AND ");
							having.append("(").append(filter.getHaving()).append(")");
						}
					}
				}
			}
			loader = new CursorLoader(getActivity(),
				Collection.buildUri(having.toString()),
				getProjection(),
				where.toString(),
				args,
				sorter == null ? null : sorter.getOrderByClause());
		} else if (id == ViewQuery._TOKEN) {
			if (viewId > 0) {
				loader = new CursorLoader(getActivity(), CollectionViews.buildViewFilterUri(viewId), ViewQuery.PROJECTION, null, null, null);
			}
		}
		return loader;
	}

	private String[] getProjection() {
		String[] projection = sorter == null ? Query.PROJECTION : StringUtils.unionArrays(Query.PROJECTION, sorter.getColumns());
		for (CollectionFilterer filter : filters) {
			projection = StringUtils.unionArrays(projection, filter.getColumns());
		}
		return projection;
	}

	private String buildDefaultWhereClause() {
		if (TextUtils.isEmpty(defaultWhereClause)) {
			defaultWhereClause = PreferenceUtils.getSyncStatusesAsSql(requireContext());
		}
		return defaultWhereClause;
	}

	@Override
	@DebugLog
	public void onLoadFinished(@NonNull Loader<Cursor> loader, @NonNull Cursor cursor) {
		if (getActivity() == null) return;

		int token = loader.getId();
		if (token == Query._TOKEN) {
			if (adapter == null) {
				adapter = new CollectionAdapter();
				listView.setAdapter(adapter);
			}
			List<CollectionItem> items = new ArrayList<>(cursor.getCount());
			if (cursor.moveToFirst()) {
				do {
					items.add(new CollectionItem(cursor, sorter));
				} while (cursor.moveToNext());
			}
			adapter.setItems(items);

			RecyclerSectionItemDecoration sectionItemDecoration =
				new RecyclerSectionItemDecoration(
					getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height),
					getSectionCallback(items),
					true
				);
			while (listView.getItemDecorationCount() > 0) {
				listView.removeItemDecorationAt(0);
			}
			listView.addItemDecoration(sectionItemDecoration);

			final int rowCount = cursor.getCount();
			final String sortDescription = sorter == null ? "" : String.format(getActivity().getString(R.string.by_prefix), sorter.getDescription());
			rowCountView.setText(String.format(Locale.getDefault(), "%,d", rowCount));
			sortDescriptionView.setText(sortDescription);
			EventBus.getDefault().post(new CollectionCountChangedEvent(rowCount));
			EventBus.getDefault().post(new CollectionSortChangedEvent(sortDescription));

			bindFilterButtons();
			invalidateMenu();
			if (rowCount > 0) {
				AnimationUtils.fadeIn(listView);
				AnimationUtils.fadeOut(emptyContainer);
			} else {
				AnimationUtils.fadeIn(emptyContainer);
				AnimationUtils.fadeOut(listView);
			}
			progressBar.hide();
		} else if (token == ViewQuery._TOKEN) {
			if (cursor.moveToFirst()) {
				viewName = cursor.getString(ViewQuery.NAME);
				sorter = getCollectionSorter(cursor.getInt(ViewQuery.SORT_TYPE));
				filters.clear();
				do {
					CollectionFilterer filter = new CollectionFiltererFactory(getActivity()).create(cursor.getInt(ViewQuery.TYPE));
					if (filter != null) {
						filter.inflate(cursor.getString(ViewQuery.DATA));
						filters.add(filter);
					}
				} while (cursor.moveToNext());
				setEmptyText();
				requery();
			}
		} else {
			Timber.d("Query complete, Not Actionable: %s", token);
			cursor.close();
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		if (adapter != null) adapter.clearItems();
	}

	@Override
	@DebugLog
	public void removeFilter(int type) {
		for (CollectionFilterer filter : filters) {
			if (filter.getType() == type) {
				filters.remove(filter);
				break;
			}
		}
		setEmptyText();
		requery();
	}

	@Override
	@DebugLog
	public void addFilter(@NotNull CollectionFilterer filter) {
		filters.remove(filter);
		if (filter.isValid()) {
			filters.add(filter);
		}
		FilterEvent.log("Collection", String.valueOf(filter.getType()));
		setEmptyText();
		requery();
	}

	@DebugLog
	private void setEmptyText() {
		if (emptyButton == null) return;
		if (PreferenceUtils.isCollectionSetToSync(getContext())) {
			final Set<String> syncedStatuses = PreferenceUtils.getSyncStatuses(getContext());
			if (SyncPrefs.noPreviousCollectionSync(requireContext())) {
				setEmptyStateForNoAction(R.string.empty_collection_sync_never);
			} else if (hasFiltersApplied()) {
				if (isAtLeastOneSyncOff(syncedStatuses, getListOfVisibleStatuses())) {
					setEmptyStateForSettingsAction(R.string.empty_collection_filter_on_sync_partial);
				} else {
					setEmptyStateForNoAction(R.string.empty_collection_filter_on);
				}
			} else {
				setEmptyStateForSettingsAction(R.string.empty_collection);
			}
		} else {
			setEmptyStateForSettingsAction(R.string.empty_collection_sync_off);
		}
	}

	private Set<String> getListOfVisibleStatuses() {
		for (CollectionFilterer filter : filters) {
			if (filter instanceof CollectionStatusFilterer) {
				return ((CollectionStatusFilterer) filter).getSelectedStatusesSet();
			}
		}
		return new HashSet<>();
	}

	private boolean isAtLeastOneSyncOff(Set<String> syncedStatuses, Set<String> statusesToCheck) {
		for (String status : statusesToCheck) {
			if (!syncedStatuses.contains(status)) {
				return true;
			}
		}
		return false;
	}

	private void setEmptyStateForSettingsAction(@StringRes int textResId) {
		emptyTextView.setText(textResId);
		emptyButton.setVisibility(View.VISIBLE);
	}

	private void setEmptyStateForNoAction(@StringRes int textResId) {
		emptyTextView.setText(textResId);
		emptyButton.setVisibility(View.GONE);
	}

	@OnClick(R.id.empty_button)
	void onSyncClick() {
		startActivity(new Intent(getContext(), SettingsActivity.class));
	}

	private boolean hasFiltersApplied() {
		return filters.size() > 0;
	}

	@DebugLog
	public void setSort(int sortType) {
		if (sortType == CollectionSorterFactory.TYPE_UNKNOWN) {
			sortType = CollectionSorterFactory.TYPE_DEFAULT;
		}
		SortEvent.log("Collection", String.valueOf(sortType));
		sorter = getCollectionSorter(sortType);
		requery();
	}

	@DebugLog
	private CollectionFilterer findFilter(int type) {
		for (CollectionFilterer filter : filters) {
			if (filter != null && filter.getType() == type) {
				return filter;
			}
		}
		return null;
	}

	@DebugLog
	private void bindFilterButtons() {
		chipGroup.removeAllViews();
		for (final CollectionFilterer filter : filters) {
			if (filter != null && !TextUtils.isEmpty(filter.toShortDescription())) {
				Chip chip = new Chip(getContext(), null, R.style.Widget_MaterialComponents_Chip_Filter);
				chip.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				chip.setText(filter.toShortDescription());
				chip.setOnClickListener(v -> launchFilterDialog(filter.getType()));
				chip.setOnLongClickListener(v -> {
					removeFilter(filter.getType());
					return true;
				});

				chipGroup.addView(chip);
			}
		}

		final boolean show = chipGroup.getChildCount() > 0;
		if (show) {
			AnimationUtils.slideUpIn(chipGroupScrollView);
		} else {
			AnimationUtils.slideDownOut(chipGroupScrollView);
		}
		swipeRefreshLayout.setPadding(
			swipeRefreshLayout.getPaddingLeft(),
			swipeRefreshLayout.getPaddingTop(),
			swipeRefreshLayout.getPaddingRight(),
			show ? getResources().getDimensionPixelSize(R.dimen.chip_group_height) : 0);
	}

	@DebugLog
	public boolean launchFilterDialog(int filterType) {
		CollectionFilterDialogFactory factory = new CollectionFilterDialogFactory();
		CollectionFilterDialog dialog = factory.create(getActivity(), filterType);
		if (dialog != null) {
			dialog.createDialog(getActivity(), this, findFilter(filterType));
			return true;
		} else {
			Timber.w("Couldn't find a filter dialog of type %s", filterType);
			return false;
		}
	}

	@DebugLog
	public long getViewId() {
		return viewId;
	}

	@DebugLog
	public void setView(long viewId) {
		if (this.viewId != viewId) {
			progressBar.show();
			this.viewId = viewId;
			LoaderManager.getInstance(this).restartLoader(ViewQuery._TOKEN, null, this);
		}
	}

	@DebugLog
	public void clearView() {
		if (viewId != 0) {
			viewId = 0;
			viewName = "";
		}
		filters.clear();
		sorter = getCollectionSorter(CollectionSorterFactory.TYPE_DEFAULT);
		requery();
	}

	public class CollectionItem {
		public final long internalId;
		public final int collectionId;
		public final String collectionName;
		public final int gameId;
		public final String gameName;
		public final int year;
		public final String collectionThumbnailUrl;
		public final String thumbnailUrl;
		public final String imageUrl;
		public final String heroImageUrl;
		public final boolean isFavorite;
		public final long timestamp;
		public final Double rating;
		public final String ratingText;
		public final String displayInfo;
		public final String headerText;
		public final boolean customPlayerSort;

		public CollectionItem(Cursor cursor, CollectionSorter sorter) {
			internalId = cursor.getLong(Query._ID);
			collectionId = cursor.getInt(Query.COLLECTION_ID);
			collectionName = cursor.getString(Query.COLLECTION_NAME);
			gameId = cursor.getInt(Query.GAME_ID);
			gameName = cursor.getString(Query.GAME_NAME);
			int y = cursor.getInt(Query.COLLECTION_YEAR_PUBLISHED);
			if (y == ConstantsKt.YEAR_UNKNOWN) {
				year = cursor.getInt(Query.YEAR_PUBLISHED);
			} else {
				year = y;
			}
			collectionThumbnailUrl = CursorUtils.getString(cursor, Query.COLLECTION_THUMBNAIL_URL);
			thumbnailUrl = CursorUtils.getString(cursor, Query.THUMBNAIL_URL);
			imageUrl = CursorUtils.getString(cursor, Query.IMAGE_URL);
			heroImageUrl = CursorUtils.getString(cursor, Query.HERO_IMAGE_URL);
			isFavorite = cursor.getInt(Query.STARRED) == 1;
			timestamp = sorter.getTimestamp(cursor);
			rating = sorter.getRating(cursor);
			ratingText = sorter.getRatingText(cursor);
			displayInfo = sorter == null ? "" : sorter.getDisplayInfo(cursor);
			headerText = sorter == null ? "" : sorter.getHeaderText(cursor, cursor.getPosition());
			customPlayerSort = cursor.getInt(Query.CUSTOM_PLAYER_SORT) == 1;
		}
	}

	public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionItemViewHolder> {
		private final List<CollectionItem> items = new ArrayList<>();
		private final SparseBooleanArray selectedItems = new SparseBooleanArray();
		private final Random random = new Random();

		@DebugLog
		public CollectionAdapter() {
			setHasStableIds(true);
		}

		public void clearItems() {
			items.clear();
			notifyDataSetChanged();
		}

		public void setItems(List<CollectionItem> values) {
			items.clear();
			items.addAll(values);
			notifyDataSetChanged();
		}

		public CollectionItem getItem(int position) {
			if (position < 0 || position >= items.size()) return null;
			return items.get(position);
		}

		public CollectionItem getRandomItem() {
			return getItem(random.nextInt(getItemCount()));
		}

		public int getSelectedItemCount() {
			return selectedItems.size();
		}

		public List<Integer> getSelectedItemPositions() {
			ArrayList<Integer> items = new ArrayList<>(selectedItems.size());
			for (int i = 0; i < selectedItems.size(); i++) {
				items.add(selectedItems.keyAt(i));
			}
			return items;
		}

		public void toggleSelection(int position) {
			if (selectedItems.get(position, false)) {
				selectedItems.delete(position);
			} else {
				selectedItems.put(position, true);
			}
			notifyDataSetChanged(); // I'd prefer to call notifyItemChanged(position), but that causes the section header to appear briefly
			if (actionMode != null) {
				int count = getSelectedItemCount();
				if (count == 0) {
					actionMode.finish();
				} else {
					actionMode.invalidate();
				}
			}
		}

		public void clearSelection() {
			selectedItems.clear();
			notifyDataSetChanged();
		}

		@Override
		public long getItemId(int position) {
			return items.get(position).internalId;
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		@NonNull
		@Override
		public CollectionItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new CollectionItemViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.row_collection, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull CollectionItemViewHolder holder, final int position) {
			CollectionItem item = getItem(position);
			holder.bindView(item, position);
		}

		public class CollectionItemViewHolder extends RecyclerView.ViewHolder {
			@BindView(R.id.name) TextView nameView;
			@BindView(R.id.year) TextView yearView;
			@BindView(R.id.info) TextView infoView;
			@BindView(R.id.timestamp) TimestampView timestampView;
			@BindView(R.id.rating) TextView ratingView;
			@BindView(R.id.thumbnail) ImageView thumbnailView;
			@BindView(R.id.favorite) ImageView favoriteView;

			public CollectionItemViewHolder(View view) {
				super(view);
				ButterKnife.bind(this, view);
			}

			public void bindView(final CollectionItem item, final int position) {
				nameView.setText(item.collectionName);
				yearView.setText(IntUtils.asYear(item.year, getContext()));
				timestampView.setTimestamp(item.timestamp);
				favoriteView.setVisibility(item.isFavorite ? View.VISIBLE : View.GONE);
				if (!TextUtils.isEmpty(item.ratingText)) {
					ratingView.setText(item.ratingText);
					ColorUtils.setTextViewBackground(ratingView, ColorUtils.getRatingColor(item.rating));
					ratingView.setVisibility(View.VISIBLE);
					infoView.setVisibility(View.GONE);
				} else {
					TextViewUtils.setTextOrHide(infoView, item.displayInfo);
					infoView.setVisibility(View.VISIBLE);
					ratingView.setVisibility(View.GONE);
				}
				ImageUtils.loadThumbnail(thumbnailView, item.collectionThumbnailUrl, item.thumbnailUrl);

				itemView.setActivated(selectedItems.get(position, false));

				itemView.setOnClickListener(v -> {
					if (isCreatingShortcut) {
						EventBus.getDefault().post(new GameShortcutRequestedEvent(item.gameId, item.gameName, item.thumbnailUrl));
					} else if (changingGamePlayId != BggContract.INVALID_ID) {
						LogPlayActivity.changeGame(getContext(), changingGamePlayId, item.gameId, item.gameName, item.thumbnailUrl, item.imageUrl, item.heroImageUrl);
						getActivity().finish(); // don't want to come back to collection activity in "pick a new game" mode
					} else if (actionMode == null) {
						GameActivity.start(requireContext(), item.gameId, item.gameName, item.thumbnailUrl, item.heroImageUrl);
					} else {
						adapter.toggleSelection(position);
					}
				});
				itemView.setOnLongClickListener(v -> {
					if (isCreatingShortcut) return false;
					if (changingGamePlayId != BggContract.INVALID_ID) return false;
					if (actionMode != null) return false;
					actionMode = getActivity().startActionMode(CollectionFragment.this);
					if (actionMode == null) return false;
					toggleSelection(position);
					return true;
				});
			}
		}
	}

	private RecyclerSectionItemDecoration.SectionCallback getSectionCallback(final List<CollectionItem> items) {
		return new RecyclerSectionItemDecoration.SectionCallback() {
			@Override
			public boolean isSection(int position) {
				if (position == RecyclerView.NO_POSITION) return false;
				if (items == null || items.size() == 0) return false;
				if (position == 0) return true;
				if (position < 0 || position >= items.size()) return false;
				String thisLetter = items.get(position).headerText;
				String lastLetter = items.get(position - 1).headerText;
				return !thisLetter.equals(lastLetter);
			}

			@NotNull
			@Override
			public CharSequence getSectionHeader(int position) {
				if (position == RecyclerView.NO_POSITION) return "-";
				if (items == null || items.size() == 0) return "-";
				if (position < 0 || position >= items.size()) return "-";
				return items.get(position).headerText;
			}
		};
	}

	private interface Query {
		int _TOKEN = 0x01;
		String[] PROJECTION = {
			Collection._ID,
			Collection.COLLECTION_ID,
			Collection.COLLECTION_NAME,
			Collection.YEAR_PUBLISHED,
			Collection.GAME_NAME,
			Games.GAME_ID,
			Collection.COLLECTION_THUMBNAIL_URL,
			Collection.THUMBNAIL_URL,
			Collection.IMAGE_URL,
			Collection.COLLECTION_YEAR_PUBLISHED,
			Games.CUSTOM_PLAYER_SORT,
			Games.STARRED,
			Collection.COLLECTION_HERO_IMAGE_URL
		};

		int _ID = 0;
		int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		int YEAR_PUBLISHED = 3;
		int GAME_NAME = 4;
		int GAME_ID = 5;
		int COLLECTION_THUMBNAIL_URL = 6;
		int THUMBNAIL_URL = 7;
		int IMAGE_URL = 8;
		int COLLECTION_YEAR_PUBLISHED = 9;
		int CUSTOM_PLAYER_SORT = 10;
		int STARRED = 11;
		int HERO_IMAGE_URL = 12;
	}

	private interface ViewQuery {
		int _TOKEN = 0x02;
		String[] PROJECTION = {
			CollectionViewFilters._ID,
			CollectionViewFilters.NAME,
			CollectionViewFilters.SORT_TYPE,
			CollectionViewFilters.TYPE,
			CollectionViewFilters.DATA
		};

		// int _ID = 0;
		int NAME = 1;
		int SORT_TYPE = 2;
		int TYPE = 3;
		int DATA = 4;
	}

	@Override
	@DebugLog
	public boolean onCreateActionMode(@NonNull ActionMode mode, @NonNull android.view.Menu menu) {
		mode.getMenuInflater().inflate(R.menu.game_context, menu);
		adapter.clearSelection();
		return true;
	}

	@Override
	@DebugLog
	public boolean onPrepareActionMode(ActionMode mode, android.view.Menu menu) {
		int count = adapter.getSelectedItemCount();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));
		menu.findItem(R.id.menu_log_play).setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		menu.findItem(R.id.menu_log_play_quick).setVisible(PreferencesUtils.showQuickLogPlay(getActivity()));
		menu.findItem(R.id.menu_link).setVisible(count == 1);
		return true;
	}

	@Override
	@DebugLog
	public void onDestroyActionMode(ActionMode mode) {
		actionMode = null;
		adapter.clearSelection();
	}

	@Override
	@DebugLog
	public boolean onActionItemClicked(@NonNull ActionMode mode, @NonNull android.view.MenuItem item) {
		if (!adapter.getSelectedItemPositions().iterator().hasNext()) {
			return false;
		}
		final CollectionItem ci = adapter.getItem(adapter.getSelectedItemPositions().iterator().next());
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				LogPlayActivity.logPlay(getContext(), ci.gameId, ci.gameName, ci.thumbnailUrl, ci.imageUrl, ci.heroImageUrl, ci.customPlayerSort);
				mode.finish();
				return true;
			case R.id.menu_log_play_quick:
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, adapter.getSelectedItemCount());
				Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
				for (int position : adapter.getSelectedItemPositions()) {
					CollectionItem collectionItem = adapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), collectionItem.gameId, collectionItem.gameName);
				}
				mode.finish();
				return true;
			case R.id.menu_share:
				final String shareMethod = "Collection";
				if (adapter.getSelectedItemCount() == 1) {
					ActivityUtils.shareGame(getActivity(), ci.gameId, ci.gameName, shareMethod);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(adapter.getSelectedItemCount());
					for (int position : adapter.getSelectedItemPositions()) {
						CollectionItem collectionItem = adapter.getItem(position);
						games.add(new Pair<>(collectionItem.gameId, collectionItem.gameName));
					}
					ActivityUtils.shareGames(getActivity(), games, shareMethod);
				}
				mode.finish();
				return true;
			case R.id.menu_link:
				ActivityUtils.linkBgg(getActivity(), ci.gameId);
				mode.finish();
				return true;
		}
		return false;
	}

	@NonNull
	private String createViewDescription(Sorter sort, List<CollectionFilterer> filters) {
		StringBuilder text = new StringBuilder();

		if (filters.size() > 0) {
			text.append(getString(R.string.filtered_by));
			for (CollectionFilterer filter : filters) {
				if (filter != null) {
					text.append("\n\u2022 ");
					text.append(filter.toLongDescription());
				}
			}
			if (sortType != CollectionSorterFactory.TYPE_DEFAULT && text.length() > 0) text.append("\n\n");
		}

		if (sortType != CollectionSorterFactory.TYPE_DEFAULT)
			text.append(getString(R.string.sort_description, sort.getDescription()));
		return text.toString();
	}

	/**
	 * Add a new view with the current filters and sort with the specified name.
	 *
	 * @return The ID of the inserted view.
	 */
	public long insertView(String name) {
		ContentValues values = new ContentValues();
		values.put(CollectionViews.NAME, name);
		values.put(CollectionViews.STARRED, false);
		values.put(CollectionViews.SORT_TYPE, sorter.getType());
		Uri filterUri = getActivity().getContentResolver().insert(CollectionViews.CONTENT_URI, values);

		int filterId = CollectionViews.getViewId(filterUri);
		Uri uri = CollectionViews.buildViewFilterUri(filterId);
		insertDetails(uri, filters);
		return StringUtils.parseLong(filterUri.getLastPathSegment());
	}

	/**
	 * Update the specified view with current filters and sort.
	 */
	public void updateView(long viewId) {
		ContentResolver resolver = getActivity().getContentResolver();

		ContentValues values = new ContentValues();
		values.put(CollectionViews.SORT_TYPE, sorter.getType());
		resolver.update(CollectionViews.buildViewUri(viewId), values, null, null);

		Uri uri = CollectionViews.buildViewFilterUri(viewId);
		resolver.delete(uri, null, null);
		insertDetails(uri, filters);
	}

	private void insertDetails(Uri viewFiltersUri, final List<CollectionFilterer> filters) {
		List<ContentValues> cvs = new ArrayList<>(filters.size());
		for (CollectionFilterer filter : filters) {
			if (filter != null) {
				ContentValues cv = new ContentValues();
				cv.put(CollectionViewFilters.TYPE, filter.getType());
				cv.put(CollectionViewFilters.DATA, filter.deflate());
				cvs.add(cv);
			}
		}
		if (cvs.size() > 0) {
			ContentValues[] values = {};
			getActivity().getContentResolver().bulkInsert(viewFiltersUri, cvs.toArray(values));
		}
	}
}
