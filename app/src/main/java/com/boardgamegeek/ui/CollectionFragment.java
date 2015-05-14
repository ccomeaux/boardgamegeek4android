package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterDataFactory;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.CollectionStatusFilterer;
import com.boardgamegeek.filterer.ExpansionStatusFilterer;
import com.boardgamegeek.interfaces.CollectionView;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.sorter.CollectionSorter;
import com.boardgamegeek.sorter.CollectionSorterFactory;
import com.boardgamegeek.ui.dialog.AverageRatingFilter;
import com.boardgamegeek.ui.dialog.AverageWeightFilter;
import com.boardgamegeek.ui.dialog.CollectionStatusFilter;
import com.boardgamegeek.ui.dialog.DeleteView;
import com.boardgamegeek.ui.dialog.ExpansionStatusFilter;
import com.boardgamegeek.ui.dialog.GeekRankingFilter;
import com.boardgamegeek.ui.dialog.GeekRatingFilter;
import com.boardgamegeek.ui.dialog.PlayCountFilter;
import com.boardgamegeek.ui.dialog.PlayTimeFilter;
import com.boardgamegeek.ui.dialog.PlayerNumberFilter;
import com.boardgamegeek.ui.dialog.SaveView;
import com.boardgamegeek.ui.dialog.SuggestedAgeFilter;
import com.boardgamegeek.ui.dialog.YearPublishedFilter;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.RandomUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import hugo.weaving.DebugLog;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import timber.log.Timber;

public class CollectionFragment extends StickyHeaderListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
	CollectionView, MultiChoiceModeListener {
	private static final String STATE_SELECTED_ID = "STATE_SELECTED_ID";
	private static final String STATE_VIEW_ID = "STATE_VIEW_ID";
	private static final String STATE_VIEW_NAME = "STATE_VIEW_NAME";
	private static final String STATE_SORT_TYPE = "STATE_SORT_TYPE";
	private static final String STATE_FILTERS = "STATE_FILTERS";
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	private int mSelectedCollectionId;
	private CollectionAdapter mAdapter;
	private long mViewId;
	private String mViewName = "";
	private CollectionSorter mSort;
	private List<CollectionFilterer> mFilters = new ArrayList<>();
	private String mDefaultWhereClause;
	private LinearLayout mFilterLinearLayout;
	private boolean mShortcut;
	private LinkedHashSet<Integer> mSelectedPositions = new LinkedHashSet<>();
	private android.view.MenuItem mLogPlayMenuItem;
	private android.view.MenuItem mLogPlayQuickMenuItem;
	private android.view.MenuItem mBggLinkMenuItem;

	public interface Callbacks {
		public boolean onGameSelected(int gameId, String gameName);

		public void onSetShortcut(Intent intent);

		public void onCollectionCountChanged(int count);

		public void onSortChanged(String sortName);

		public void onViewRequested(long viewId);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onGameSelected(int gameId, String gameName) {
			return true;
		}

		@Override
		public void onSetShortcut(Intent intent) {
		}

		@Override
		public void onCollectionCountChanged(int count) {
		}

		@Override
		public void onSortChanged(String sortName) {
		}

		@Override
		public void onViewRequested(long viewId) {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		if (savedInstanceState != null) {
			mSelectedCollectionId = savedInstanceState.getInt(STATE_SELECTED_ID);
			mViewId = savedInstanceState.getLong(STATE_VIEW_ID);
			mViewName = savedInstanceState.getString(STATE_VIEW_NAME);
			mFilters = savedInstanceState.getParcelableArrayList(STATE_FILTERS);
		}
		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mShortcut = "android.intent.action.CREATE_SHORTCUT".equals(intent.getAction());
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_collection, container, false);
	}

	@Override
	@DebugLog
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mFilterLinearLayout = (LinearLayout) view.findViewById(R.id.filter_linear_layout);
		setEmptyText();
	}

	@Override
	@DebugLog
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		int sortType = CollectionSorterFactory.TYPE_DEFAULT;
		if (savedInstanceState != null) {
			sortType = savedInstanceState.getInt(STATE_SORT_TYPE);
		}
		mSort = CollectionSorterFactory.create(sortType, getActivity());
		if (savedInstanceState != null || mShortcut) {
			requery();
		}
		ActionMode.setMultiChoiceMode(getListView().getWrappedList(), getActivity(), this);
	}

	@DebugLog
	private void requery() {
		getLoaderManager().restartLoader(Query._TOKEN, null, this);
	}

	@Override
	@DebugLog
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		if (mUpdaterRunnable != null) {
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	@DebugLog
	public void onPause() {
		super.onPause();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
	}


	@Override
	@DebugLog
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(STATE_VIEW_ID, mViewId);
		outState.putString(STATE_VIEW_NAME, mViewName);
		outState.putInt(STATE_SORT_TYPE, mSort == null ? CollectionSorterFactory.TYPE_UNKNOWN : mSort.getType());
		outState.putParcelableArrayList(STATE_FILTERS, (ArrayList<? extends Parcelable>) mFilters);
		if (mSelectedCollectionId > 0) {
			outState.putInt(STATE_SELECTED_ID, mSelectedCollectionId);
		}
	}

	@Override
	@DebugLog
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	@DebugLog
	public void onListItemClick(View view, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int gameId = cursor.getInt(Query.GAME_ID);
		final String gameName = cursor.getString(Query.COLLECTION_NAME);
		final String thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL);
		if (mShortcut) {
			Intent shortcut = ShortcutUtils.createIntent(getActivity(), gameId, gameName, thumbnailUrl);
			mCallbacks.onSetShortcut(shortcut);
		} else {
			if (mCallbacks.onGameSelected(gameId, gameName)) {
				setSelectedGameId(gameId);
			}
		}
	}

	@Override
	@DebugLog
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.collection_fragment, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	@DebugLog
	public void onPrepareOptionsMenu(Menu menu) {
		DrawerActivity drawerActivity = ((DrawerActivity) getActivity());
		if (drawerActivity != null && drawerActivity.isDrawerOpen()) {
			menu.findItem(R.id.menu_collection_sort).setVisible(false);
			menu.findItem(R.id.menu_collection_filter).setVisible(false);
			menu.findItem(R.id.menu_collection_random_game).setVisible(false);
			menu.findItem(R.id.menu_collection_view_save).setVisible(false);
			menu.findItem(R.id.menu_collection_view_delete).setVisible(false);
		} else {
			menu.findItem(R.id.menu_collection_sort).setVisible(true);
			menu.findItem(R.id.menu_collection_filter).setVisible(true);

			if (mShortcut) {
				menu.findItem(R.id.menu_collection_random_game).setVisible(false);
				menu.findItem(R.id.menu_collection_view_save).setVisible(false);
				menu.findItem(R.id.menu_collection_view_delete).setVisible(false);
			} else {
				menu.findItem(R.id.menu_collection_random_game).setVisible(true);
				menu.findItem(R.id.menu_collection_view_save).setVisible(true);
				menu.findItem(R.id.menu_collection_view_delete).setVisible(true);

				menu.findItem(R.id.menu_collection_random_game).setEnabled(
					mAdapter != null && mAdapter.getCount() > 0);

				menu.findItem(R.id.menu_collection_view_save).setEnabled(
					(mFilters != null && mFilters.size() > 0)
						|| (mSort != null && mSort.getType() != CollectionSorterFactory.TYPE_DEFAULT));

				boolean hasViews = false;
				Activity activity = getActivity();
				if (activity != null) {
					hasViews = ResolverUtils.getCount(activity.getContentResolver(), CollectionViews.CONTENT_URI) > 0;
				}
				menu.findItem(R.id.menu_collection_view_delete).setEnabled(hasViews);

			}
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	@DebugLog
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_collection_random_game:
				final Cursor cursor = (Cursor) mAdapter.getItem(RandomUtils.getRandom().nextInt(mAdapter.getCount()));
				ActivityUtils.launchGame(getActivity(), cursor.getInt(Query.GAME_ID), cursor.getString(Query.COLLECTION_NAME));
				return true;
			case R.id.menu_collection_view_save:
				SaveView.createDialog(getActivity(), this, mViewName, mSort, mFilters);
				return true;
			case R.id.menu_collection_view_delete:
				DeleteView.createDialog(getActivity(), this);
				return true;
			case R.id.menu_collection_sort_name:
				setSort(CollectionSorterFactory.TYPE_COLLECTION_NAME);
				return true;
			case R.id.menu_collection_sort_rank:
				setSort(CollectionSorterFactory.TYPE_RANK);
				return true;
			case R.id.menu_collection_sort_geek_rating:
				setSort(CollectionSorterFactory.TYPE_GEEK_RATING);
				return true;
			case R.id.menu_collection_sort_rating:
				setSort(CollectionSorterFactory.TYPE_AVERAGE_RATING);
				return true;
			case R.id.menu_collection_sort_myrating:
				setSort(CollectionSorterFactory.TYPE_MY_RATING);
				return true;
			case R.id.menu_collection_sort_last_viewed:
				setSort(CollectionSorterFactory.TYPE_LAST_VIEWED);
				return true;
			case R.id.menu_collection_sort_wishlist_priority:
				setSort(CollectionSorterFactory.TYPE_WISHLIST_PRIORITY);
				return true;
			case R.id.menu_collection_sort_published:
				setSort(CollectionSorterFactory.TYPE_YEAR_PUBLISHED_DESC, CollectionSorterFactory.TYPE_YEAR_PUBLISHED_ASC);
				return true;
			case R.id.menu_collection_sort_playtime:
				setSort(CollectionSorterFactory.TYPE_PLAY_TIME_ASC, CollectionSorterFactory.TYPE_PLAY_TIME_DESC);
				return true;
			case R.id.menu_collection_sort_age:
				setSort(CollectionSorterFactory.TYPE_AGE_ASC, CollectionSorterFactory.TYPE_AGE_DESC);
				return true;
			case R.id.menu_collection_sort_weight:
				setSort(CollectionSorterFactory.TYPE_AVERAGE_WEIGHT_ASC, CollectionSorterFactory.TYPE_AVERAGE_WEIGHT_DESC);
				return true;
			case R.id.menu_collection_sort_plays:
				setSort(CollectionSorterFactory.TYPE_PLAY_COUNT_DESC, CollectionSorterFactory.TYPE_PLAY_COUNT_ASC);
				return true;
			case R.id.menu_collection_sort_acquisition_date:
				setSort(CollectionSorterFactory.TYPE_ACQUISITION_DATE);
				return true;
		}

		return launchFilterDialog(item.getItemId()) || super.onOptionsItemSelected(item);
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			StringBuilder where = new StringBuilder();
			String[] args = {};
			Builder uriBuilder = Collection.CONTENT_URI.buildUpon();
			if (mViewId == 0 && mFilters == null || mFilters.size() == 0) {
				where.append(buildDefaultWhereClause());
			} else {
				for (CollectionFilterer filter : mFilters) {
					if (filter != null) {
						if (!TextUtils.isEmpty(filter.getSelection())) {
							if (where.length() > 0) {
								where.append(" AND ");
							}
							where.append("(").append(filter.getSelection()).append(")");
							args = StringUtils.concatenate(args, filter.getSelectionArgs());
						}
					}
				}
			}
			Uri mUri = uriBuilder.build();
			loader = new CursorLoader(getActivity(), mUri, mSort == null ? Query.PROJECTION : StringUtils.unionArrays(
				Query.PROJECTION, mSort.getColumns()), where.toString(), args, mSort == null ? null
				: mSort.getOrderByClause());
		} else if (id == ViewQuery._TOKEN) {
			if (mViewId > 0) {
				loader = new CursorLoader(getActivity(), CollectionViews.buildViewFilterUri(mViewId),
					ViewQuery.PROJECTION, null, null, null);
			}
		}
		return loader;
	}

	private String buildDefaultWhereClause() {
		if (!TextUtils.isEmpty(mDefaultWhereClause)) {
			return mDefaultWhereClause;
		}
		String[] statuses = PreferencesUtils.getSyncStatuses(getActivity());
		if (statuses == null) {
			mDefaultWhereClause = "";
			return mDefaultWhereClause;
		}
		StringBuilder where = new StringBuilder();
		for (String status : statuses) {
			if (TextUtils.isEmpty(status)) {
				continue;
			}
			if (where.length() > 0) {
				where.append(" OR ");
			}
			switch (status) {
				case "own":
					where.append(Collection.STATUS_OWN).append("=1");
					break;
				case "played":
					where.append(Games.NUM_PLAYS).append(">0");
					break;
				case "rated":
					where.append(Collection.RATING).append(">0");
					break;
				case "comment":
					where.append(Collection.COMMENT).append("='' OR ").append(Collection.COMMENT).append(" IS NULL");
					break;
				case "prevowned":
					where.append(Collection.STATUS_PREVIOUSLY_OWNED).append("=1");
					break;
				case "trade":
					where.append(Collection.STATUS_FOR_TRADE).append("=1");
					break;
				case "want":
					where.append(Collection.STATUS_WANT).append("=1");
					break;
				case "wanttobuy":
					where.append(Collection.STATUS_WANT_TO_BUY).append("=1");
					break;
				case "wishlist":
					where.append(Collection.STATUS_WISHLIST).append("=1");
					break;
				case "wanttoplay":
					where.append(Collection.STATUS_WANT_TO_PLAY).append("=1");
					break;
				case "preordered":
					where.append(Collection.STATUS_PREORDERED).append("=1");
					break;
				case "hasparts":
					where.append(Collection.HASPARTS_LIST).append("='' OR ").append(Collection.HASPARTS_LIST).append(" IS NULL");
					break;
				case "wantparts":
					where.append(Collection.WANTPARTS_LIST).append("='' OR ").append(Collection.WANTPARTS_LIST).append(" IS NULL");
					break;
			}
		}
		mDefaultWhereClause = where.toString();
		return mDefaultWhereClause;
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == Query._TOKEN) {
			if (mAdapter == null) {
				mAdapter = new CollectionAdapter(getActivity());
				setListAdapter(mAdapter);
			} else {
				setProgressShown(false);
			}
			mAdapter.changeCursor(cursor);
			initializeTimeBasedUi();
			restoreScrollState();
			mCallbacks.onCollectionCountChanged(cursor.getCount());
			mCallbacks.onSortChanged(mSort == null ? "" : mSort.getDescription());
			bindFilterButtons();
		} else if (token == ViewQuery._TOKEN) {
			if (cursor.moveToFirst()) {
				mViewName = cursor.getString(ViewQuery.NAME);
				mSort = CollectionSorterFactory.create(cursor.getInt(ViewQuery.SORT_TYPE), getActivity());
				mFilters.clear();
				do {
					CollectionFilterer filter = CollectionFilterDataFactory.create(getActivity(),
						cursor.getInt(ViewQuery.TYPE), cursor.getString(ViewQuery.DATA));
					mFilters.add(filter);
				} while (cursor.moveToNext());
				setEmptyText();
				requery();
			}
		} else {
			Timber.d("Query complete, Not Actionable: " + token);
			cursor.close();
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
		if (mAdapter != null) {
			mAdapter.changeCursor(null);
		}
	}

	protected void initializeTimeBasedUi() {
		updateTimeBasedUi();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
		mUpdaterRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
	}

	protected void updateTimeBasedUi() {
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@DebugLog
	public void setSelectedGameId(int id) {
		mSelectedCollectionId = id;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	@DebugLog
	public void removeFilter(CollectionFilterer filter) {
		mFilters.remove(filter);
		setEmptyText();
		resetScrollState();
		requery();
	}

	@Override
	@DebugLog
	public void addFilter(CollectionFilterer filter) {
		mFilters.remove(filter);
		if (filter.isValid()) {
			mFilters.add(filter);
		}
		setEmptyText();
		resetScrollState();
		requery();
	}

	@DebugLog
	private void setEmptyText() {
		int resId = R.string.empty_collection;
		if (mFilters != null && mFilters.size() > 0) {
			resId = R.string.empty_collection_filter_on;
		} else {
			String[] statuses = PreferencesUtils.getSyncStatuses(getActivity());
			if (statuses == null || statuses.length == 0) {
				resId = R.string.empty_collection_sync_off;
			}
		}
		setEmptyText(getString(resId));
	}

	@DebugLog
	private void setSort(int sortType) {
		if (sortType == CollectionSorterFactory.TYPE_UNKNOWN) {
			sortType = CollectionSorterFactory.TYPE_DEFAULT;
		}
		mSort = CollectionSorterFactory.create(sortType, getActivity());
		resetScrollState();
		requery();
	}

	@DebugLog
	private void setSort(int sortType, int sortType2) {
		if (mSort.getType() == sortType) {
			setSort(sortType2);
		} else {
			setSort(sortType);
		}
	}

	@Override
	@DebugLog
	public void createView(long id, String name) {
		Toast.makeText(getActivity(), R.string.msg_saved, Toast.LENGTH_SHORT).show();
		mCallbacks.onViewRequested(id);
	}

	@Override
	@DebugLog
	public void deleteView(long id) {
		Toast.makeText(getActivity(), R.string.msg_collection_view_deleted, Toast.LENGTH_SHORT).show();
		if (mViewId == id) {
			mCallbacks.onViewRequested(PreferencesUtils.getViewDefaultId(getActivity()));
		}
	}

	@DebugLog
	private CollectionFilterer findFilter(int type) {
		for (CollectionFilterer filter : mFilters) {
			if (filter != null && filter.getType() == type) {
				return filter;
			}
		}
		return null;
	}

	@DebugLog
	private void bindFilterButtons() {
		final LayoutInflater layoutInflater = getLayoutInflater(null);
		for (CollectionFilterer filter : mFilters) {
			if (filter != null) {
				Button button = (Button) mFilterLinearLayout.findViewWithTag(filter.getType());
				if (button == null) {
					mFilterLinearLayout.addView(createFilterButton(layoutInflater, filter.getType(),
						filter.getDisplayText()));
				} else {
					button.setText(filter.getDisplayText());
				}
			}
		}

		// Could be when button is clicked, but this keeps filters synced with collection
		for (int i = 0; i < mFilterLinearLayout.getChildCount(); i++) {
			Button button = (Button) mFilterLinearLayout.getChildAt(i);
			if (!mFilters.contains(new CollectionFilterer((Integer) button.getTag()))) {
				mFilterLinearLayout.removeView(button);
				i--;
			}
		}
	}

	@DebugLog
	private Button createFilterButton(LayoutInflater layoutInflater, final int type, String text) {
		final Button button = (Button) layoutInflater
			.inflate(R.layout.widget_button_filter, mFilterLinearLayout, false);
		button.setText(text);
		button.setTag(type);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT);
		int margin = getResources().getDimensionPixelSize(R.dimen.padding_small);
		params.setMargins(margin, margin, margin, margin);
		button.setLayoutParams(params);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchFilterDialog((Integer) v.getTag());
			}
		});
		button.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				removeFilter(new CollectionFilterer(type));
				return true;
			}
		});
		return button;
	}

	@DebugLog
	private boolean launchFilterDialog(int id) {
		switch (id) {
			case R.id.menu_collection_status:
			case CollectionFilterDataFactory.TYPE_COLLECTION_STATUS:
				CollectionStatusFilterer filter = null;
				try {
					filter = (CollectionStatusFilterer) findFilter(CollectionFilterDataFactory.TYPE_COLLECTION_STATUS);
				} catch (ClassCastException e) {
					// Getting reports of this, but don't know why
					Timber.i("ClassCastException when attempting to display the CollectionStatusFilter dialog.");
				}
				new CollectionStatusFilter().createDialog(getActivity(), this, filter);
				return true;
			case R.id.menu_expansion_status:
			case CollectionFilterDataFactory.TYPE_EXPANSION_STATUS:
				new ExpansionStatusFilter().createDialog(getActivity(), this,
					(ExpansionStatusFilterer) findFilter(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS));
				return true;
			case R.id.menu_number_of_players:
			case CollectionFilterDataFactory.TYPE_PLAYER_NUMBER:
				new PlayerNumberFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_PLAYER_NUMBER));
				return true;
			case R.id.menu_play_time:
			case CollectionFilterDataFactory.TYPE_PLAY_TIME:
				new PlayTimeFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_PLAY_TIME));
				return true;
			case R.id.menu_suggested_age:
			case CollectionFilterDataFactory.TYPE_SUGGESTED_AGE:
				new SuggestedAgeFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_SUGGESTED_AGE));
				return true;
			case R.id.menu_average_weight:
			case CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT:
				new AverageWeightFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT));
				return true;
			case R.id.menu_year_published:
			case CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED:
				new YearPublishedFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED));
				return true;
			case R.id.menu_average_rating:
			case CollectionFilterDataFactory.TYPE_AVERAGE_RATING:
				new AverageRatingFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_AVERAGE_RATING));
				return true;
			case R.id.menu_geek_rating:
			case CollectionFilterDataFactory.TYPE_GEEK_RATING:
				new GeekRatingFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_GEEK_RATING));
				return true;
			case R.id.menu_geek_ranking:
			case CollectionFilterDataFactory.TYPE_GEEK_RANKING:
				new GeekRankingFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_GEEK_RANKING));
				return true;
			case R.id.menu_play_count:
			case CollectionFilterDataFactory.TYPE_PLAY_COUNT:
				new PlayCountFilter().createDialog(getActivity(), this,
					findFilter(CollectionFilterDataFactory.TYPE_PLAY_COUNT));
				return true;

		}
		return false;
	}

	@DebugLog
	public long getViewId() {
		return mViewId;
	}

	@DebugLog
	public void setView(long viewId) {
		if (mViewId != viewId) {
			setProgressShown(true);
			mViewId = viewId;
			resetScrollState();
			getLoaderManager().restartLoader(ViewQuery._TOKEN, null, this);
		}
	}

	@DebugLog
	public void clearView() {
		setProgressShown(true);
		mViewId = 0;
		mViewName = "";
		resetScrollState();
		mFilters.clear();
		mSort = CollectionSorterFactory.create(CollectionSorterFactory.TYPE_DEFAULT, getActivity());
		requery();
	}

	private class CollectionAdapter extends CursorAdapter implements StickyListHeadersAdapter {
		private LayoutInflater mInflater;

		@DebugLog
		public CollectionAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
		}

		@Override
		@DebugLog
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_collection, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		@DebugLog
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			int collectionId = cursor.getInt(Query.COLLECTION_ID);
			int year = cursor.getInt(Query.COLLECTION_YEAR_PUBLISHED);
			if (year == 0) {
				year = cursor.getInt(Query.YEAR_PUBLISHED);
			}
			String yearText;
			if (year > 0) {
				yearText = getString(R.string.year_positive, year);
			} else if (year == 0) {
				yearText = getString(R.string.year_zero, year);
			} else {
				yearText = getString(R.string.year_negative, -year);
			}
			String collectionThumbnailUrl = cursor.getString(Query.COLLECTION_THUMBNAIL_URL);
			String thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL);

			UIUtils.setActivatedCompat(view, collectionId == mSelectedCollectionId);

			holder.name.setText(cursor.getString(Query.COLLECTION_NAME));
			holder.year.setText(yearText);
			holder.info.setText(mSort == null ? "" : mSort.getDisplayInfo(cursor));
			loadThumbnail(!TextUtils.isEmpty(collectionThumbnailUrl) ? collectionThumbnailUrl : thumbnailUrl,
				holder.thumbnail);
		}

		@Override
		public long getHeaderId(int position) {
			if (position < 0) {
				return 0;
			}
			return mSort.getHeaderId(getCursor(), position);
		}

		@Override
		public View getHeaderView(int position, View convertView, ViewGroup parent) {
			HeaderViewHolder holder;
			if (convertView == null) {
				holder = new HeaderViewHolder();
				convertView = mInflater.inflate(R.layout.row_header, parent, false);
				holder.text = (TextView) convertView.findViewById(android.R.id.title);
				convertView.setTag(holder);
			} else {
				holder = (HeaderViewHolder) convertView.getTag();
			}
			holder.text.setText(mSort.getHeaderText(getCursor(), position));
			return convertView;
		}

		class ViewHolder {
			TextView name;
			TextView year;
			TextView info;
			ImageView thumbnail;

			public ViewHolder(View view) {
				name = (TextView) view.findViewById(R.id.name);
				year = (TextView) view.findViewById(R.id.year);
				info = (TextView) view.findViewById(R.id.info);
				thumbnail = (ImageView) view.findViewById(R.id.list_thumbnail);
			}
		}

		class HeaderViewHolder {
			TextView text;
		}
	}

	private interface Query {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Collection._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME,
			Collection.YEAR_PUBLISHED, Collection.GAME_NAME, Games.GAME_ID, Collection.COLLECTION_THUMBNAIL_URL,
			Collection.THUMBNAIL_URL, Collection.IMAGE_URL, Collection.COLLECTION_YEAR_PUBLISHED,
			Games.CUSTOM_PLAYER_SORT };

		// int _ID = 0;
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
	}

	private interface ViewQuery {
		int _TOKEN = 0x02;
		String[] PROJECTION = { CollectionViewFilters._ID, CollectionViewFilters.NAME, CollectionViewFilters.SORT_TYPE,
			CollectionViewFilters.TYPE, CollectionViewFilters.DATA, };

		// int _ID = 0;
		int NAME = 1;
		int SORT_TYPE = 2;
		int TYPE = 3;
		int DATA = 4;
	}

	@Override
	@DebugLog
	public boolean onCreateActionMode(ActionMode mode, android.view.Menu menu) {
		android.view.MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.game_context, menu);
		mLogPlayMenuItem = menu.findItem(R.id.menu_log_play);
		mLogPlayQuickMenuItem = menu.findItem(R.id.menu_log_play_quick);
		mBggLinkMenuItem = menu.findItem(R.id.menu_link);
		mSelectedPositions.clear();
		return true;
	}

	@Override
	@DebugLog
	public boolean onPrepareActionMode(ActionMode mode, android.view.Menu menu) {
		return false;
	}

	@Override
	@DebugLog
	public void onDestroyActionMode(ActionMode mode) {
	}

	@Override
	@DebugLog
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if (checked) {
			mSelectedPositions.add(position);
		} else {
			mSelectedPositions.remove(position);
		}

		int count = mSelectedPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));

		mLogPlayMenuItem.setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		mLogPlayQuickMenuItem.setVisible(PreferencesUtils.showQuickLogPlay(getActivity()));
		mBggLinkMenuItem.setVisible(count == 1);
	}

	@Override
	@DebugLog
	public boolean onActionItemClicked(ActionMode mode, android.view.MenuItem item) {
		Cursor cursor = (Cursor) mAdapter.getItem(mSelectedPositions.iterator().next());
		int gameId = cursor.getInt(Query.GAME_ID);
		String gameName = cursor.getString(Query.GAME_NAME);
		String thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL);
		String imageUrl = cursor.getString(Query.IMAGE_URL);
		boolean customPlayerSort = (cursor.getInt(Query.CUSTOM_PLAYER_SORT) == 1);
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), gameId, gameName, thumbnailUrl, imageUrl, customPlayerSort);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				String text = getResources().getQuantityString(R.plurals.msg_logging_plays, mSelectedPositions.size());
				Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
				for (int position : mSelectedPositions) {
					Cursor c = (Cursor) mAdapter.getItem(position);
					ActivityUtils.logQuickPlay(getActivity(), c.getInt(Query.GAME_ID), c.getString(Query.GAME_NAME));
				}
				return true;
			case R.id.menu_share:
				mode.finish();
				if (mSelectedPositions.size() == 1) {
					ActivityUtils.shareGame(getActivity(), gameId, gameName);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<>(mSelectedPositions.size());
					for (int position : mSelectedPositions) {
						Cursor c = (Cursor) mAdapter.getItem(position);
						games.add(new Pair<>(c.getInt(Query.GAME_ID), c.getString(Query.GAME_NAME)));
					}
					ActivityUtils.shareGames(getActivity(), games);
				}
				return true;
			case R.id.menu_link:
				mode.finish();
				ActivityUtils.linkBgg(getActivity(), gameId);
				return true;
		}
		return false;
	}
}
