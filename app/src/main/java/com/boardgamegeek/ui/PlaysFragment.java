package com.boardgamegeek.ui;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.events.PlaysFilterChangedEvent;
import com.boardgamegeek.events.PlaysSortChangedEvent;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.extensions.TaskUtils;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.sorter.PlaysSorter;
import com.boardgamegeek.sorter.PlaysSorterFactory;
import com.boardgamegeek.tasks.sync.SyncPlaysByDateTask;
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask;
import com.boardgamegeek.ui.model.PlayModel;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.ui.widget.RecyclerSectionItemDecoration;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.fabric.FilterEvent;
import com.boardgamegeek.util.fabric.SortEvent;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import timber.log.Timber;

public class PlaysFragment extends Fragment implements
	LoaderCallbacks<Cursor>,
	ActionMode.Callback,
	OnDateSetListener,
	OnRefreshListener {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL";
	private static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	private static final String KEY_ICON_COLOR = "ICON_COLOR";
	private static final String KEY_MODE = "MODE";
	private static final String KEY_MODE_VALUE = "MODE_VALUE";
	public static final int FILTER_TYPE_STATUS_ALL = -2;
	public static final int FILTER_TYPE_STATUS_UPDATE = 1;
	public static final int FILTER_TYPE_STATUS_DIRTY = 2;
	public static final int FILTER_TYPE_STATUS_DELETE = 3;
	public static final int FILTER_TYPE_STATUS_PENDING = 4;
	private static final int MODE_ALL = 0;
	private static final int MODE_GAME = 1;
	private static final int MODE_BUDDY = 2;
	private static final int MODE_PLAYER = 3;
	private static final int MODE_LOCATION = 4;
	private static final int PLAY_QUERY_TOKEN = 0x21;
	private static final int HELP_VERSION = 2;

	private PlayAdapter adapter;
	private Uri uri;
	private int gameId;
	private String gameName;
	private String thumbnailUrl;
	private String imageUrl;
	private String heroImageUrl;
	private boolean arePlayersCustomSorted;
	private int filterType = FILTER_TYPE_STATUS_ALL;
	private PlaysSorter sorter;
	private boolean hasAutoSyncTriggered;
	private int mode = MODE_ALL;
	private String modeValue;
	private ShowcaseView showcaseView;
	private boolean isSyncing = false;
	private ActionMode actionMode = null;

	private Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.progress) ContentLoadingProgressBar progressBar;
	@BindView(android.R.id.list) RecyclerView listView;
	@BindView(R.id.empty_container) ViewGroup emptyContainer;
	@BindView(android.R.id.empty) TextView emptyTextView;
	@BindView(R.id.fab) FloatingActionButton fabView;

	public static PlaysFragment newInstance() {
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, MODE_ALL);
		PlaysFragment fragment = new PlaysFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static PlaysFragment newInstanceForGame(int gameId, String gameName, String imageUrl, String thumbnailUrl, String heroImageUrl, boolean arePlayersCustomSorted, @ColorInt int iconColor) {
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, MODE_GAME);
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);
		args.putString(KEY_IMAGE_URL, imageUrl);
		args.putString(KEY_THUMBNAIL_URL, thumbnailUrl);
		args.putString(KEY_HERO_IMAGE_URL, heroImageUrl);
		args.putBoolean(KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
		args.putInt(KEY_ICON_COLOR, iconColor);
		PlaysFragment fragment = new PlaysFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static PlaysFragment newInstanceForLocation(String locationName) {
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, MODE_LOCATION);
		args.putString(KEY_MODE_VALUE, locationName);
		PlaysFragment fragment = new PlaysFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static PlaysFragment newInstanceForBuddy(String username) {
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, MODE_BUDDY);
		args.putString(KEY_MODE_VALUE, username);
		PlaysFragment fragment = new PlaysFragment();
		fragment.setArguments(args);
		return fragment;
	}

	public static PlaysFragment newInstanceForPlayer(String playerName) {
		Bundle args = new Bundle();
		args.putInt(KEY_MODE, MODE_PLAYER);
		args.putString(KEY_MODE_VALUE, playerName);
		PlaysFragment fragment = new PlaysFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_plays, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);

		listView.setLayoutManager(new LinearLayoutManager(getContext()));
		listView.setHasFixedSize(true);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
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

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int sortType = PlaysSorterFactory.TYPE_DEFAULT;
		sorter = PlaysSorterFactory.create(getContext(), sortType);

		uri = Plays.CONTENT_URI;
		Bundle bundle = getArguments();

		mode = bundle.getInt(KEY_MODE, mode);
		switch (mode) {
			case MODE_GAME:
				gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
				gameName = bundle.getString(KEY_GAME_NAME);
				thumbnailUrl = bundle.getString(KEY_THUMBNAIL_URL);
				imageUrl = bundle.getString(KEY_IMAGE_URL);
				heroImageUrl = bundle.getString(KEY_HERO_IMAGE_URL);
				arePlayersCustomSorted = bundle.getBoolean(KEY_CUSTOM_PLAYER_SORT);
				LoaderManager.getInstance(this).restartLoader(GameQuery._TOKEN, bundle, this);
				break;
			case MODE_BUDDY:
				modeValue = bundle.getString(PlaysFragment.KEY_MODE_VALUE);
				this.uri = Plays.buildPlayersByPlayUri();
				break;
			case MODE_PLAYER:
				modeValue = bundle.getString(PlaysFragment.KEY_MODE_VALUE);
				this.uri = Plays.buildPlayersByPlayUri();
				break;
			case MODE_LOCATION:
				modeValue = bundle.getString(PlaysFragment.KEY_MODE_VALUE);
				break;
		}
		@ColorInt int iconColor = bundle.getInt(KEY_ICON_COLOR, Color.TRANSPARENT);

		PresentationUtils.colorFab(fabView, iconColor);
		showFab(mode == MODE_GAME);

		setEmptyText(getString(getEmptyStringResource()));
		requery();

		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());
		swipeRefreshLayout.setOnRefreshListener(this);

		maybeShowHelp();
	}

	public void setEmptyText(CharSequence text) {
		emptyTextView.setText(text);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		switch (filterType) {
			case FILTER_TYPE_STATUS_DIRTY:
				UIUtils.checkMenuItem(menu, R.id.menu_filter_in_progress);
				break;
			case FILTER_TYPE_STATUS_PENDING:
				UIUtils.checkMenuItem(menu, R.id.menu_filter_pending);
				break;
			case FILTER_TYPE_STATUS_ALL:
			default:
				UIUtils.checkMenuItem(menu, R.id.menu_filter_all);
				break;
		}
		if (sorter != null) {
			switch (sorter.getType()) {
				case PlaysSorterFactory.TYPE_PLAY_DATE:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_date);
					break;
				case PlaysSorterFactory.TYPE_PLAY_GAME:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_game);
					break;
				case PlaysSorterFactory.TYPE_PLAY_LENGTH:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_length);
					break;
				case PlaysSorterFactory.TYPE_PLAY_LOCATION:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_location);
					break;
				default:
					UIUtils.checkMenuItem(menu, R.id.menu_sort_date);
					break;
			}
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String title = item.getTitle().toString();
		switch (item.getItemId()) {
			case R.id.menu_sort_date:
				setSort(PlaysSorterFactory.TYPE_PLAY_DATE);
				return true;
			case R.id.menu_sort_location:
				setSort(PlaysSorterFactory.TYPE_PLAY_LOCATION);
				return true;
			case R.id.menu_sort_game:
				setSort(PlaysSorterFactory.TYPE_PLAY_GAME);
				return true;
			case R.id.menu_sort_length:
				setSort(PlaysSorterFactory.TYPE_PLAY_LENGTH);
				return true;
			case R.id.menu_filter_all:
				filter(FILTER_TYPE_STATUS_ALL, title);
				return true;
			case R.id.menu_filter_in_progress:
				filter(FILTER_TYPE_STATUS_DIRTY, title);
				return true;
			case R.id.menu_filter_pending:
				filter(FILTER_TYPE_STATUS_PENDING, title);
				return true;
			case R.id.menu_refresh_on:
				DatePickerFragment datePickerFragment = new DatePickerFragment();
				datePickerFragment.setListener(this);
				datePickerFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
				return true;
			case R.id.menu_help:
				showHelp();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(SyncCompleteEvent event) {
		isSyncing(false);
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPlaysByDateTask.CompletedEvent event) {
		isSyncing(false);
	}

	protected void isSyncing(boolean value) {
		isSyncing = value;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) {
						swipeRefreshLayout.setRefreshing(isSyncing);
					}
				}
			});
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPlaysByGameTask.CompletedEvent event) {
		if (mode == MODE_GAME && event.getGameId() == gameId) {
			isSyncing(false);
			if (!TextUtils.isEmpty(event.getErrorMessage())) {
				Toast.makeText(getContext(), event.getErrorMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	protected void showFab(boolean show) {
		if (fabView == null) return;
		if (show) {
			fabView.show();
		} else {
			fabView.hide();
		}
	}

	private void showHelp() {
		final Builder builder = HelpUtils.getShowcaseBuilder(getActivity());
		if (builder != null) {
			showcaseView = builder
				.setContentText(R.string.help_plays)
				.setTarget(Target.NONE)
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						showcaseView.hide();
						HelpUtils.updateHelp(getContext(), HelpUtils.HELP_PLAYS_KEY, HELP_VERSION);
					}
				})
				.build();
			showcaseView.show();
		}
	}

	private void maybeShowHelp() {
		if (HelpUtils.shouldShowHelp(getContext(), HelpUtils.HELP_PLAYS_KEY, HELP_VERSION)) {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					showHelp();
				}
			}, 100);
		}
	}

	private void requery() {
		if (mode == MODE_ALL || mode == MODE_LOCATION || mode == MODE_GAME) {
			LoaderManager.getInstance(this).restartLoader(SumQuery._TOKEN, getArguments(), this);
		} else if (mode == MODE_PLAYER || mode == MODE_BUDDY) {
			LoaderManager.getInstance(this).restartLoader(PlayerSumQuery._TOKEN, getArguments(), this);
		}
		LoaderManager.getInstance(this).restartLoader(PLAY_QUERY_TOKEN, getArguments(), this);
	}

	@SuppressWarnings("unused")
	@Subscribe(sticky = true)
	public void onEvent(PlaysSortChangedEvent event) {
		setSort(event.getType());
	}

	@SuppressWarnings("unused")
	@Subscribe(sticky = true)
	public void onEvent(PlaysFilterChangedEvent event) {
		filter(event.getType(), event.getDescription());
	}

	private void setSort(int sortType) {
		if (sorter != null && sortType == sorter.getType()) {
			return;
		}
		if (sortType == PlaysSorterFactory.TYPE_UNKNOWN) {
			sortType = PlaysSorterFactory.TYPE_DEFAULT;
		}
		SortEvent.log("Plays", String.valueOf(sortType));
		sorter = PlaysSorterFactory.create(getContext(), sortType);
		requery();
	}

	public static class DatePickerFragment extends DialogFragment {
		private OnDateSetListener listener;

		@Override
		@NonNull
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final Calendar calendar = Calendar.getInstance();
			return new DatePickerDialog(getActivity(),
				listener,
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH));
		}

		public void setListener(OnDateSetListener listener) {
			this.listener = listener;
		}
	}

	@Override
	public void onDateSet(DatePicker view, int year, int month, int day) {
		isSyncing(true);
		String date = DateTimeUtils.formatDateForApi(year, month, day);
		TaskUtils.executeAsyncTask(new SyncPlaysByDateTask((BggApplication) getActivity().getApplication(), date));
	}

	private int getEmptyStringResource() {
		switch (mode) {
			case MODE_BUDDY:
				return R.string.empty_plays_buddy;
			case MODE_PLAYER:
				return R.string.empty_plays_player;
			case MODE_LOCATION:
				return R.string.empty_plays_location;
			case MODE_GAME:
				return R.string.empty_plays_game;
			case MODE_ALL:
			default:
				switch (filterType) {
					case FILTER_TYPE_STATUS_DIRTY:
						return R.string.empty_plays_draft;
					case FILTER_TYPE_STATUS_UPDATE:
						return R.string.empty_plays_update;
					case FILTER_TYPE_STATUS_DELETE:
						return R.string.empty_plays_delete;
					case FILTER_TYPE_STATUS_PENDING:
						return R.string.empty_plays_pending;
					case FILTER_TYPE_STATUS_ALL:
					default:
						if (PreferencesUtils.getSyncPlays(getActivity())) {
							return R.string.empty_plays_sync_off;
						} else {
							return R.string.empty_plays_sync_off;
						}
				}
		}
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case PLAY_QUERY_TOKEN:
				loader = new CursorLoader(getContext(),
					uri,
					sorter == null ? PlayModel.getProjection() : StringUtils.unionArrays(PlayModel.getProjection(), sorter.getColumns()),
					selection(),
					selectionArgs(),
					sorter == null ? null : sorter.getOrderByClause());
				loader.setUpdateThrottle(2000);
				break;
			case GameQuery._TOKEN:
				loader = new CursorLoader(getContext(), Games.buildGameUri(gameId), GameQuery.PROJECTION, null, null, null);
				loader.setUpdateThrottle(0);
				break;
			case SumQuery._TOKEN:
				loader = new CursorLoader(getContext(), Plays.CONTENT_SIMPLE_URI, SumQuery.PROJECTION, selection(), selectionArgs(), null);
				loader.setUpdateThrottle(0);
				break;
			case PlayerSumQuery._TOKEN:
				Uri uri = Plays.buildPlayersByUniquePlayerUri();
				if (mode == MODE_BUDDY) {
					uri = Plays.buildPlayersByUniqueUserUri();
				}
				loader = new CursorLoader(getContext(), uri, PlayerSumQuery.PROJECTION, selection(), selectionArgs(), null);
				loader.setUpdateThrottle(0);
				break;
		}
		return loader;
	}

	private String selection() {
		switch (mode) {
			case MODE_ALL:
				if (filterType == FILTER_TYPE_STATUS_ALL) {
					return null;
				} else if (filterType == FILTER_TYPE_STATUS_PENDING) {
					return String.format("%s>0 OR %s>0", Plays.DELETE_TIMESTAMP, Plays.UPDATE_TIMESTAMP);
				} else {
					return String.format("%s>0", Plays.DIRTY_TIMESTAMP);
				}
			case MODE_GAME:
				return Plays.OBJECT_ID + "=?";
			case MODE_BUDDY:
				return PlayPlayers.USER_NAME + "=?";
			case MODE_PLAYER:
				return PlayPlayers.USER_NAME + "='' AND play_players." + PlayPlayers.NAME + "=?";
			case MODE_LOCATION:
				return Plays.LOCATION + "=?";
		}
		return null;
	}

	private String[] selectionArgs() {
		switch (mode) {
			case MODE_ALL:
				return null;
			case MODE_GAME:
				return new String[] { String.valueOf(gameId) };
			case MODE_BUDDY:
			case MODE_PLAYER:
			case MODE_LOCATION:
				return new String[] { modeValue };
		}
		return null;
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == PLAY_QUERY_TOKEN) {
			if (adapter == null) {
				adapter = new PlayAdapter(getContext());
				listView.setAdapter(adapter);
			}
			List<PlayModel> plays = new ArrayList<>();
			if (cursor.moveToFirst()) {
				do {
					plays.add(PlayModel.fromCursor(cursor, getContext()));
				} while (cursor.moveToNext());
			}
			adapter.changeData(plays);

			RecyclerSectionItemDecoration sectionItemDecoration =
				new RecyclerSectionItemDecoration(
					getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height),
					getSectionCallback(plays, sorter),
					true
				);
			while (listView.getItemDecorationCount() > 0) {
				listView.removeItemDecorationAt(0);
			}
			listView.addItemDecoration(sectionItemDecoration);

			if (cursor.getCount() == 0) {
				AnimationUtils.fadeIn(emptyContainer);
				AnimationUtils.fadeOut(listView);
			} else {
				AnimationUtils.fadeIn(listView);
				AnimationUtils.fadeOut(emptyContainer);
			}

			PlaysSortChangedEvent event = sorter == null ?
				new PlaysSortChangedEvent(PlaysSorterFactory.TYPE_UNKNOWN, "") :
				new PlaysSortChangedEvent(sorter.getType(), sorter.getDescription());
			EventBus.getDefault().postSticky(event);
			progressBar.hide();
		} else if (token == GameQuery._TOKEN) {
			if (!hasAutoSyncTriggered && cursor != null && cursor.moveToFirst()) {
				hasAutoSyncTriggered = true;
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
			EventBus.getDefault().postSticky(new PlaysCountChangedEvent(count));
		} else if (token == PlayerSumQuery._TOKEN) {
			int count = 0;
			if (cursor != null && cursor.moveToFirst()) {
				count = cursor.getInt(PlayerSumQuery.SUM_QUANTITY);
			}
			EventBus.getDefault().postSticky(new PlaysCountChangedEvent(count));
		} else {
			Timber.d("Query complete, Not Actionable: %s", token);
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		if (loader.getId() == PLAY_QUERY_TOKEN) {
			adapter.clear();
		}
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(@NonNull SyncEvent event) {
		if ((event.getType() & SyncService.FLAG_SYNC_PLAYS) == SyncService.FLAG_SYNC_PLAYS) {
			isSyncing(true);
		}
	}

	public void triggerRefresh() {
		if (isSyncing) return;
		switch (mode) {
			case MODE_ALL:
			case MODE_BUDDY:
			case MODE_PLAYER:
			case MODE_LOCATION:
				SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS);
				break;
			case MODE_GAME:
				isSyncing(true);
				SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS_UPLOAD);
				TaskUtils.executeAsyncTask(new SyncPlaysByGameTask((BggApplication) getActivity().getApplication(), gameId));
				break;
		}
	}

	@OnClick(R.id.fab)
	protected void onFabClicked() {
		LogPlayActivity.logPlay(getContext(), gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted);
	}

	public void filter(int type, String description) {
		if (type != filterType && mode == MODE_ALL) {
			filterType = type;
			FilterEvent.log("Plays", String.valueOf(type));
			EventBus.getDefault().postSticky(new PlaysFilterChangedEvent(filterType, description));
			setEmptyText(getString(getEmptyStringResource()));
			requery();
		}
	}

	class PlayAdapter extends RecyclerView.Adapter<PlayAdapter.ViewHolder> {
		private final LayoutInflater inflater;
		private final List<PlayModel> plays = new ArrayList<>();
		private final SparseBooleanArray selectedItems = new SparseBooleanArray();

		public PlayAdapter(Context context) {
			setHasStableIds(true);
			inflater = LayoutInflater.from(context);
		}

		public void clear() {
			plays.clear();
		}

		public void changeData(List<PlayModel> plays) {
			this.plays.clear();
			this.plays.addAll(plays);
			notifyDataSetChanged();
		}

		public PlayModel getItem(int position) {
			return plays.get(position);
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

		public boolean areAllSelectedItemsPending() {
			for (int pos : adapter.getSelectedItemPositions()) {
				PlayModel play = adapter.getItem(pos);
				boolean pending = play.getDirtyTimestamp() > 0;
				if (!pending) return false;
			}
			return true;
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
		public int getItemCount() {
			return plays.size();
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).getInternalId();
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder(inflater.inflate(R.layout.row_play, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
			final PlayModel play = plays.get(position);

			String info = PresentationUtils.describePlayDetails(getActivity(),
				mode != MODE_GAME ? play.getDate() : null,
				play.getLocation(), play.getQuantity(), play.getLength(), play.getPlayerCount());

			if (mode != MODE_GAME) {
				holder.title.setText(play.getName());
			} else {
				holder.title.setText(play.getDate());
			}
			PresentationUtils.setTextOrHide(holder.text1, info);
			PresentationUtils.setTextOrHide(holder.text2, play.getComments());

			int statusMessageId = 0;
			if (play.getDeleteTimestamp() > 0) {
				statusMessageId = R.string.sync_pending_delete;
			} else if (play.getUpdateTimestamp() > 0) {
				statusMessageId = R.string.sync_pending_update;
			} else if (play.getDirtyTimestamp() > 0) {
				if (play.getPlayId() > 0) {
					statusMessageId = R.string.sync_editing;
				} else {
					statusMessageId = R.string.sync_draft;
				}
			}
			if (statusMessageId == 0) {
				holder.status.setVisibility(View.GONE);
			} else {
				holder.status.setVisibility(View.VISIBLE);
				holder.status.setText(statusMessageId);
			}

			holder.itemView.setActivated(selectedItems.get(position, false));

			holder.itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (actionMode == null) {
						PlayActivity.start(getContext(), play.getInternalId(), play.getGameId(), play.getName(), play.getThumbnailUrl(), play.getImageUrl(), play.getHeroImageUrl());
					} else {
						toggleSelection(position);
					}
				}
			});
			holder.itemView.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					if (actionMode != null) return false;
					actionMode = getActivity().startActionMode(PlaysFragment.this);
					if (actionMode == null) return false;
					toggleSelection(position);
					return true;
				}
			});
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			@BindView(android.R.id.title) TextView title;
			@BindView(android.R.id.text1) TextView text1;
			@BindView(android.R.id.text2) TextView text2;
			@BindView(android.R.id.message) TextView status;

			public ViewHolder(View view) {
				super(view);
				ButterKnife.bind(this, view);
			}
		}
	}

	private RecyclerSectionItemDecoration.SectionCallback getSectionCallback(final List<PlayModel> plays, final PlaysSorter sorter) {
		return new RecyclerSectionItemDecoration.SectionCallback() {
			@Override
			public boolean isSection(int position) {
				if (position == RecyclerView.NO_POSITION) return false;
				if (plays == null || plays.size() == 0) return false;
				if (position == 0) return true;
				if (position < 0 || position >= plays.size()) return false;
				String thisLetter = sorter.getSectionText(plays.get(position));
				String lastLetter = sorter.getSectionText(plays.get(position - 1));
				return !thisLetter.equals(lastLetter);
			}

			@NotNull
			@Override
			public CharSequence getSectionHeader(int position) {
				if (position == RecyclerView.NO_POSITION) return "-";
				if (plays == null || plays.size() == 0) return "-";
				if (position < 0 || position >= plays.size()) return "-";
				return sorter.getSectionText(plays.get(position));
			}
		};
	}

	private interface GameQuery {
		int _TOKEN = 0x22;
		String[] PROJECTION = { Games.UPDATED_PLAYS };
		int UPDATED_PLAYS = 0;
	}

	private interface SumQuery {
		int _TOKEN = 0x23;
		String[] PROJECTION = { Plays.SUM_QUANTITY };
		int TOTAL_COUNT = 0;
	}

	private interface PlayerSumQuery {
		int _TOKEN = 0x24;
		String[] PROJECTION = { PlayPlayers.SUM_QUANTITY };

		int SUM_QUANTITY = 0;
	}

	// TODO Add support for share option

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.getMenuInflater().inflate(R.menu.plays_context, menu);
		adapter.clearSelection();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		int count = adapter.getSelectedItemCount();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_plays_selected, count, count));
		menu.findItem(R.id.menu_send).setVisible(adapter.areAllSelectedItemsPending());
		menu.findItem(R.id.menu_edit).setVisible(adapter.getSelectedItemCount() == 1);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		actionMode = null;
		adapter.clearSelection();
	}

	@Override
	public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
		if (!adapter.getSelectedItemPositions().iterator().hasNext()) return false;
		switch (item.getItemId()) {
			case R.id.menu_send:
				new AlertDialog.Builder(getContext())
					.setMessage(getResources().getQuantityString(R.plurals.are_you_sure_send_play, adapter.getSelectedItemCount()))
					.setCancelable(true)
					.setNegativeButton(R.string.cancel, null)
					.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							updateSelectedPlays(Plays.UPDATE_TIMESTAMP, System.currentTimeMillis());
							mode.finish();
						}
					})
					.show();
				return true;
			case R.id.menu_edit:
				PlayModel play = adapter.getItem(adapter.getSelectedItemPositions().iterator().next());
				LogPlayActivity.editPlay(getActivity(), play.getInternalId(), play.getGameId(), play.getName(), play.getThumbnailUrl(), play.getImageUrl(), play.getHeroImageUrl());
				mode.finish();
				return true;
			case R.id.menu_delete:
				new AlertDialog.Builder(getContext())
					.setMessage(getResources().getQuantityString(R.plurals.are_you_sure_delete_play, adapter.getSelectedItemCount()))
					.setCancelable(true)
					.setNegativeButton(R.string.cancel, null)
					.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							updateSelectedPlays(Plays.DELETE_TIMESTAMP, System.currentTimeMillis());
							mode.finish();
						}
					})
					.show();
				return true;
		}
		return false;
	}

	private void updateSelectedPlays(String key, long value) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		for (int position : adapter.getSelectedItemPositions()) {
			PlayModel play = adapter.getItem(position);
			if (play.getInternalId() != BggContract.INVALID_ID)
				batch.add(ContentProviderOperation
					.newUpdate(Plays.buildPlayUri(play.getInternalId()))
					.withValue(key, value)
					.build());
		}
		ResolverUtils.applyBatch(getActivity(), batch);
		SyncService.sync(getActivity(), SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}
}
