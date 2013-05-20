package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.data.AverageRatingFilterData;
import com.boardgamegeek.data.AverageWeightFilterData;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.CollectionFilterDataFactory;
import com.boardgamegeek.data.CollectionNameSortData;
import com.boardgamegeek.data.CollectionSortData;
import com.boardgamegeek.data.CollectionSortDataFactory;
import com.boardgamegeek.data.CollectionStatusFilterData;
import com.boardgamegeek.data.CollectionView;
import com.boardgamegeek.data.ExpansionStatusFilterData;
import com.boardgamegeek.data.GeekRankingFilterData;
import com.boardgamegeek.data.GeekRatingFilterData;
import com.boardgamegeek.data.PlayTimeFilterData;
import com.boardgamegeek.data.PlayerNumberFilterData;
import com.boardgamegeek.data.SuggestedAgeFilterData;
import com.boardgamegeek.data.YearPublishedFilterData;
import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.dialog.AverageRatingFilter;
import com.boardgamegeek.ui.dialog.AverageWeightFilter;
import com.boardgamegeek.ui.dialog.CollectionStatusFilter;
import com.boardgamegeek.ui.dialog.DeleteFilters;
import com.boardgamegeek.ui.dialog.ExpansionStatusFilter;
import com.boardgamegeek.ui.dialog.GeekRankingFilter;
import com.boardgamegeek.ui.dialog.GeekRatingFilter;
import com.boardgamegeek.ui.dialog.PlayTimeFilter;
import com.boardgamegeek.ui.dialog.PlayerNumberFilter;
import com.boardgamegeek.ui.dialog.SaveFilters;
import com.boardgamegeek.ui.dialog.SuggestedAgeFilter;
import com.boardgamegeek.ui.dialog.YearPublishedFilter;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.actionmodecompat.ActionMode;
import com.boardgamegeek.util.actionmodecompat.MultiChoiceModeListener;

public class CollectionFragment extends BggListFragment implements AbsListView.OnScrollListener,
	LoaderManager.LoaderCallbacks<Cursor>, CollectionView, MultiChoiceModeListener {
	private static final String TAG = makeLogTag(CollectionFragment.class);
	private static final String STATE_SELECTED_ID = "STATE_SELECTED_ID";
	private static final String STATE_VIEW_ID = "STATE_VIEW_ID";
	private static final String STATE_VIEW_NAME = "STATE_VIEW_NAME";
	private static final String STATE_SORT_TYPE = "STATE_SORT_TYPE";
	private static final String STATE_FILTERS = "STATE_FILTERS";
	private static final String STATE_VIEW_MODIFIED = "STATE_VIEW_MODIFIED";

	private int mSelectedCollectionId;
	private boolean mFastScrollLetterEnabled;
	private CollectionAdapter mAdapter;
	private long mViewId;
	private String mViewName = "";
	private CollectionSortData mSort;
	private List<CollectionFilterData> mFilters = new ArrayList<CollectionFilterData>();
	private boolean mViewModified = false;

	private View mProgressView;
	private View mListContainer;
	private LinearLayout mFilterLinearLayout;
	private TextView mFastScrollLetter;
	private boolean mShortcut;

	private LinkedHashSet<Integer> mSelectedPositions = new LinkedHashSet<Integer>();
	private android.view.MenuItem mLogPlayMenuItem;
	private android.view.MenuItem mLogPlayQuickMenuItem;
	private android.view.MenuItem mBggLinkMenuItem;

	public interface Callbacks {
		public boolean onGameSelected(int gameId, String gameName);

		public void onSetShortcut(Intent intent);

		public void onCollectionCountChanged(int count);

		public void onSortChanged(String sortName);

		public void onViewRequested(long viewId);

		public boolean isNavigationDrawerOpen();
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

		@Override
		public boolean isNavigationDrawerOpen() {
			return false;
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mSelectedCollectionId = savedInstanceState.getInt(STATE_SELECTED_ID);
			mViewId = savedInstanceState.getLong(STATE_VIEW_ID);
			mViewName = savedInstanceState.getString(STATE_VIEW_NAME);
			mFilters = savedInstanceState.getParcelableArrayList(STATE_FILTERS);
			mViewModified = savedInstanceState.getBoolean(STATE_VIEW_MODIFIED);
		}
		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mShortcut = "android.intent.action.CREATE_SHORTCUT".equals(intent.getAction());

		new Handler().post(new Runnable() {
			@Override
			public void run() {
				mFastScrollLetterEnabled = true;
			};
		});
	}

	@Override
	protected int getLoadingImage() {
		return R.drawable.thumbnail_image_empty;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_collection, null);

		mProgressView = rootView.findViewById(R.id.progress);
		mListContainer = rootView.findViewById(R.id.list_container);
		mFilterLinearLayout = (LinearLayout) rootView.findViewById(R.id.filter_linear_layout);
		mFastScrollLetter = (TextView) rootView.findViewById(R.id.fast_scroll_letter);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final ListView listView = getListView();
		listView.setOnScrollListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (savedInstanceState == null) {
			mSort = new CollectionNameSortData(getActivity());
		} else {
			mSort = CollectionSortDataFactory.create(savedInstanceState.getInt(STATE_SORT_TYPE), getActivity());
		}
		requery(); // This should be handled by the activity (I think)

		ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
	}

	private void requery() {
		getLoaderManager().restartLoader(Query._TOKEN, null, this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onResume() {
		super.onResume();
		mFastScrollLetterEnabled = true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(STATE_VIEW_ID, mViewId);
		outState.putString(STATE_VIEW_NAME, mViewName);
		outState.putInt(STATE_SORT_TYPE, mSort == null ? CollectionSortDataFactory.TYPE_UNKNOWN : mSort.getType());
		outState.putParcelableArrayList(STATE_FILTERS, (ArrayList<? extends Parcelable>) mFilters);
		outState.putBoolean(STATE_VIEW_MODIFIED, mViewModified);
		if (mSelectedCollectionId > 0) {
			outState.putInt(STATE_SELECTED_ID, mSelectedCollectionId);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onPause() {
		super.onPause();
		mFastScrollLetter.setVisibility(View.INVISIBLE);
		mFastScrollLetterEnabled = false;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int gameId = cursor.getInt(Query.GAME_ID);
		final String gameName = cursor.getString(Query.COLLECTION_NAME);
		if (mShortcut) {
			Intent shortcut = ActivityUtils.createShortcut(getActivity(), gameId, gameName);
			mCallbacks.onSetShortcut(shortcut);
		} else {
			if (mCallbacks.onGameSelected(gameId, gameName)) {
				setSelectedGameId(gameId);
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.collection_fragment, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (mCallbacks.isNavigationDrawerOpen()) {
			menu.findItem(R.id.menu_collection_sort).setVisible(false);
			menu.findItem(R.id.menu_collection_filter).setVisible(false);
			menu.findItem(R.id.menu_collection_random_game).setVisible(false);
			menu.findItem(R.id.menu_collection_view_save).setVisible(false);
			menu.findItem(R.id.menu_collection_view_delete).setVisible(false);
		} else {
			menu.findItem(R.id.menu_collection_sort).setVisible(true);
			menu.findItem(R.id.menu_collection_filter).setVisible(true);

			boolean hasViews = ResolverUtils.getCount(getActivity().getContentResolver(), CollectionViews.CONTENT_URI) > 0;
			menu.findItem(R.id.menu_collection_view_delete).setEnabled(hasViews);

			menu.findItem(R.id.menu_collection_view_save).setEnabled(
				mViewModified && mFilters != null && mFilters.size() > 0);

			final MenuItem item = menu.findItem(R.id.menu_collection_random_game);
			item.setVisible(!mShortcut);
			item.setEnabled(mAdapter == null ? false : mAdapter.getCount() > 0);

			menu.findItem(R.id.menu_collection_view_save).setVisible(!mShortcut);
			menu.findItem(R.id.menu_collection_view_delete).setVisible(!mShortcut);
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_collection_random_game:
				final Cursor cursor = (Cursor) mAdapter.getItem(UIUtils.getRandom().nextInt(mAdapter.getCount()));
				ActivityUtils.launchGame(getActivity(), cursor.getInt(Query.GAME_ID),
					cursor.getString(Query.COLLECTION_NAME));
				return true;
			case R.id.menu_collection_view_save:
				SaveFilters.createDialog(getActivity(), this, mViewName, mSort, mFilters);
				return true;
			case R.id.menu_collection_view_delete:
				DeleteFilters.createDialog(getActivity(), this);
				return true;
			case R.id.menu_collection_sort_name:
				setSort(CollectionSortDataFactory.TYPE_COLLECTION_NAME);
				return true;
			case R.id.menu_collection_sort_rating:
				setSort(CollectionSortDataFactory.TYPE_GEEK_RATING);
				return true;
			case R.id.menu_collection_sort_wishlist_priority:
				setSort(CollectionSortDataFactory.TYPE_WISHLIST_PRIORITY);
				return true;
			case R.id.menu_collection_sort_published_newest:
				setSort(CollectionSortDataFactory.TYPE_YEAR_PUBLISHED_DESC);
				return true;
			case R.id.menu_collection_sort_published_oldest:
				setSort(CollectionSortDataFactory.TYPE_YEAR_PUBLISHED_ASC);
				return true;
			case R.id.menu_collection_sort_playtime_shortest:
				setSort(CollectionSortDataFactory.TYPE_PLAY_TIME_ASC);
				return true;
			case R.id.menu_collection_sort_playtime_longest:
				setSort(CollectionSortDataFactory.TYPE_PLAY_TIME_DESC);
				return true;
			case R.id.menu_collection_sort_age_youngest:
				setSort(CollectionSortDataFactory.TYPE_AGE_ASC);
				return true;
			case R.id.menu_collection_sort_age_oldest:
				setSort(CollectionSortDataFactory.TYPE_AGE_DESC);
				return true;
			case R.id.menu_collection_sort_weight_lighest:
				setSort(CollectionSortDataFactory.TYPE_AVERAGE_WEIGHT_ASC);
				return true;
			case R.id.menu_collection_sort_weight_heaviest:
				setSort(CollectionSortDataFactory.TYPE_AVERAGE_WEIGHT_DESC);
				return true;
				// case R.id.menu_collection_sort_played_most:
				// setSort(CollectionSortDataFactory.TYPE_PLAY_COUNT_DESC);
				// return true;
				// case R.id.menu_collection_sort_played_least:
				// setSort(CollectionSortDataFactory.TYPE_PLAY_COUNT_ASC);
				// return true;
		}

		if (launchFilterDialog(item.getItemId())) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (mFastScrollLetterEnabled && mAdapter != null) {
			final Cursor cursor = (Cursor) mAdapter.getItem(firstVisibleItem);
			if (cursor != null && cursor.getCount() > 0) {
				mFastScrollLetter.setText(mSort == null ? "" : mSort.getScrollText(cursor));
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView listView, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE) {
			mFastScrollLetter.setVisibility(View.GONE);
		} else {
			mFastScrollLetter.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			StringBuilder where = new StringBuilder();
			String[] args = {};
			Builder uriBuilder = Collection.CONTENT_URI.buildUpon();
			for (CollectionFilterData filter : mFilters) {
				if (!TextUtils.isEmpty(filter.getSelection())) {
					if (where.length() > 0) {
						where.append(" AND ");
					}
					where.append("(").append(filter.getSelection()).append(")");
					args = StringUtils.concat(args, filter.getSelectionArgs());
				}
				if (!TextUtils.isEmpty(filter.getPath())) {
					uriBuilder.appendPath(filter.getPath());
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

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new CollectionAdapter(getActivity());
			setListAdapter(mAdapter);
		}
		boolean load = true;
		int token = loader.getId();
		if (token == Query._TOKEN) {
			mAdapter.changeCursor(cursor);
			restoreScrollState();
			mCallbacks.onSortChanged(mSort == null ? "" : mSort.getDescription());
			bindFilterButtons();
		} else if (token == ViewQuery._TOKEN) {
			if (cursor.moveToFirst()) {
				mViewName = cursor.getString(ViewQuery.NAME);
				mSort = CollectionSortDataFactory.create(cursor.getInt(ViewQuery.SORT_TYPE), getActivity());
				mFilters.clear();
				do {
					CollectionFilterData filter = CollectionFilterDataFactory.create(getActivity(),
						cursor.getInt(ViewQuery.TYPE), cursor.getString(ViewQuery.DATA));
					mFilters.add(filter);
				} while (cursor.moveToNext());
				mViewModified = false;
				requery();
				load = false;
			}
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}
		if (load) {
			mCallbacks.onCollectionCountChanged(cursor.getCount());
			AnimationUtils.fadeOut(getActivity(), mProgressView, isResumed());
			AnimationUtils.fadeIn(getActivity(), mListContainer, isResumed());
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	public void setSelectedGameId(int id) {
		mSelectedCollectionId = id;
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void removeFilter(CollectionFilterData filter) {
		mViewModified = true;
		mFilters.remove(filter);
		requery();
	}

	@Override
	public void addFilter(CollectionFilterData filter) {
		mViewModified = true;
		mFilters.remove(filter);
		if (filter.isValid()) {
			mFilters.add(filter);
		}
		requery();
	}

	@Override
	public void setSort(int sortType) {
		mViewModified = true;
		if (sortType == CollectionSortDataFactory.TYPE_UNKNOWN) {
			sortType = CollectionSortDataFactory.TYPE_DEFAULT;
		}
		mSort = CollectionSortDataFactory.create(sortType, getActivity());
		requery();
	}

	@Override
	public void createView(long id, String name) {
		Toast.makeText(getActivity(), R.string.msg_saved, Toast.LENGTH_SHORT).show();
		mCallbacks.onViewRequested(id);
	}

	@Override
	public void deleteView(long id) {
		Toast.makeText(getActivity(), R.string.msg_collection_view_deleted, Toast.LENGTH_SHORT).show();
		if (mViewId == id) {
			mCallbacks.onViewRequested(0);
		}
	}

	private CollectionFilterData findFilter(int type) {
		for (CollectionFilterData filter : mFilters) {
			if (filter.getType() == type) {
				return filter;
			}
		}
		return null;
	}

	private void bindFilterButtons() {
		final LayoutInflater layoutInflater = getLayoutInflater(null);
		for (CollectionFilterData filter : mFilters) {
			Button button = (Button) mFilterLinearLayout.findViewWithTag(filter.getType());
			if (button == null) {
				mFilterLinearLayout.addView(createFilterButton(layoutInflater, filter.getType(),
					filter.getDisplayText()));
			} else {
				button.setText(filter.getDisplayText());
			}
		}

		// Could be when button is clicked, but this keeps filters synced with collection
		for (int i = 0; i < mFilterLinearLayout.getChildCount(); i++) {
			Button button = (Button) mFilterLinearLayout.getChildAt(i);
			if (!mFilters.contains(new CollectionFilterData((Integer) button.getTag()))) {
				mFilterLinearLayout.removeView(button);
				i--;
			}
		}
	}

	private Button createFilterButton(LayoutInflater layoutInflater, final int type, String text) {
		final Button button = (Button) layoutInflater.inflate(R.layout.widget_button_filter, null);
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
				removeFilter(new CollectionFilterData(type));
				return true;
			}
		});
		return button;
	}

	private boolean launchFilterDialog(int id) {
		switch (id) {
			case R.id.menu_collection_status:
			case CollectionFilterDataFactory.TYPE_COLLECTION_STATUS:
				new CollectionStatusFilter().createDialog(getActivity(), this,
					(CollectionStatusFilterData) findFilter(CollectionFilterDataFactory.TYPE_COLLECTION_STATUS));
				return true;
			case R.id.menu_expansion_status:
			case CollectionFilterDataFactory.TYPE_EXPANSION_STATUS:
				new ExpansionStatusFilter().createDialog(getActivity(), this,
					(ExpansionStatusFilterData) findFilter(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS));
				return true;
			case R.id.menu_number_of_players:
			case CollectionFilterDataFactory.TYPE_PLAYER_NUMBER:
				new PlayerNumberFilter().createDialog(getActivity(), this,
					(PlayerNumberFilterData) findFilter(CollectionFilterDataFactory.TYPE_PLAYER_NUMBER));
				return true;
			case R.id.menu_play_time:
			case CollectionFilterDataFactory.TYPE_PLAY_TIME:
				new PlayTimeFilter().createDialog(getActivity(), this,
					(PlayTimeFilterData) findFilter(CollectionFilterDataFactory.TYPE_PLAY_TIME));
				return true;
			case R.id.menu_suggested_age:
			case CollectionFilterDataFactory.TYPE_SUGGESTED_AGE:
				new SuggestedAgeFilter().createDialog(getActivity(), this,
					(SuggestedAgeFilterData) findFilter(CollectionFilterDataFactory.TYPE_SUGGESTED_AGE));
				return true;
			case R.id.menu_average_weight:
			case CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT:
				new AverageWeightFilter().createDialog(getActivity(), this,
					(AverageWeightFilterData) findFilter(CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT));
				return true;
			case R.id.menu_year_published:
			case CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED:
				new YearPublishedFilter().createDialog(getActivity(), this,
					(YearPublishedFilterData) findFilter(CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED));
				return true;
			case R.id.menu_average_rating:
			case CollectionFilterDataFactory.TYPE_AVERAGE_RATING:
				new AverageRatingFilter().createDialog(getActivity(), this,
					(AverageRatingFilterData) findFilter(CollectionFilterDataFactory.TYPE_AVERAGE_RATING));
				return true;
			case R.id.menu_geek_rating:
			case CollectionFilterDataFactory.TYPE_GEEK_RATING:
				new GeekRatingFilter().createDialog(getActivity(), this,
					(GeekRatingFilterData) findFilter(CollectionFilterDataFactory.TYPE_GEEK_RATING));
				return true;
			case R.id.menu_geek_ranking:
			case CollectionFilterDataFactory.TYPE_GEEK_RANKING:
				new GeekRankingFilter().createDialog(getActivity(), this,
					(GeekRankingFilterData) findFilter(CollectionFilterDataFactory.TYPE_GEEK_RANKING));
				return true;
		}
		return false;
	}

	public void setView(long viewId) {
		if (mViewId != viewId) {
			mViewId = viewId;
			getLoaderManager().restartLoader(ViewQuery._TOKEN, null, this);
		}
	}

	public void clearView() {
		if (mViewId != 0) {
			mViewId = 0;
			mViewName = "";
			mFilters.clear();
			mSort = CollectionSortDataFactory.create(CollectionSortDataFactory.TYPE_DEFAULT, getActivity());
			mViewModified = false;
			requery();
		}
	}

	private class CollectionAdapter extends CursorAdapter {
		private static final int STATE_UNKNOWN = 0;
		private static final int STATE_SECTIONED_CELL = 1;
		private static final int STATE_REGULAR_CELL = 2;

		private LayoutInflater mInflater;
		private int[] mCellStates;
		private String previousSection = "";
		private String mCurrentSection = "";
		private final String mUnknownYear = getResources().getString(R.string.text_unknown);

		public CollectionAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
		}

		@Override
		public Cursor swapCursor(Cursor newCursor) {
			mCellStates = newCursor == null ? null : new int[newCursor.getCount()];
			return super.swapCursor(newCursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_collection, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();

			boolean needSeparator = false;
			final int position = cursor.getPosition();
			mCurrentSection = mSort.getScrollText(cursor);
			switch (mCellStates[position]) {
				case STATE_SECTIONED_CELL:
					needSeparator = true;
					break;
				case STATE_REGULAR_CELL:
					needSeparator = false;
					break;
				case STATE_UNKNOWN:
				default:
					if (position == 0) {
						needSeparator = true;
					} else {
						cursor.moveToPosition(position - 1);
						previousSection = mSort.getScrollText(cursor);
						if (!previousSection.equals(mCurrentSection)) {
							needSeparator = true;
						}
						cursor.moveToPosition(position);
					}
					mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
					break;
			}

			if (needSeparator) {
				holder.separator.setText(mCurrentSection);
				holder.separator.setVisibility(View.VISIBLE);
			} else {
				holder.separator.setVisibility(View.GONE);
			}

			int collectionId = cursor.getInt(Query.COLLECTION_ID);
			int gameId = cursor.getInt(Query.GAME_ID);
			int year = cursor.getInt(Query.YEAR_PUBLISHED);
			String collectionThumbnailUrl = cursor.getString(Query.COLLECTION_THUMBNAIL_URL);
			String thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL);

			UIUtils.setActivatedCompat(view, collectionId == mSelectedCollectionId);

			holder.name.setText(cursor.getString(Query.COLLECTION_NAME));
			holder.year.setText((year > 0) ? String.valueOf(year) : mUnknownYear);
			holder.info.setText(mSort == null ? "" : mSort.getDisplayInfo(cursor));
			if (!TextUtils.isEmpty(collectionThumbnailUrl)) {
				getImageFetcher().loadThumnailImage(collectionThumbnailUrl, Collection.buildThumbnailUri(collectionId),
					holder.thumbnail);
			} else {
				getImageFetcher().loadThumnailImage(thumbnailUrl, Games.buildThumbnailUri(gameId), holder.thumbnail);
			}
		}
	}

	static class ViewHolder {
		TextView name;
		TextView year;
		TextView info;
		BezelImageView thumbnail;
		Uri thumbnailUrl;
		TextView separator;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			info = (TextView) view.findViewById(R.id.info);
			thumbnail = (BezelImageView) view.findViewById(R.id.list_thumbnail);
			separator = (TextView) view.findViewById(R.id.separator);
		}
	}

	private interface Query {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Collection._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME,
			Collection.YEAR_PUBLISHED, Collection.GAME_NAME, Games.GAME_ID, Collection.COLLECTION_THUMBNAIL_URL,
			Collection.THUMBNAIL_URL };

		// int _ID = 0;
		int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		int YEAR_PUBLISHED = 3;
		int GAME_NAME = 4;
		int GAME_ID = 5;
		int COLLECTION_THUMBNAIL_URL = 6;
		int THUMBNAIL_URL = 7;
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
	public boolean onPrepareActionMode(ActionMode mode, android.view.Menu menu) {
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
		if (checked) {
			mSelectedPositions.add(position);
		} else {
			mSelectedPositions.remove(position);
		}

		int count = mSelectedPositions.size();
		mode.setTitle(getResources().getQuantityString(R.plurals.msg_games_selected, count, count));

		mLogPlayMenuItem.setVisible(count == 1 && PreferencesUtils.showLogPlay(getActivity()));
		mLogPlayQuickMenuItem.setVisible(count == 1 && PreferencesUtils.showQuickLogPlay(getActivity()));
		mBggLinkMenuItem.setVisible(count == 1);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, android.view.MenuItem item) {
		Cursor cursor = (Cursor) mAdapter.getItem(mSelectedPositions.iterator().next());
		int gameId = cursor.getInt(Query.GAME_ID);
		String gameName = cursor.getString(Query.GAME_NAME);
		switch (item.getItemId()) {
			case R.id.menu_log_play:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), false, gameId, gameName);
				return true;
			case R.id.menu_log_play_quick:
				mode.finish();
				ActivityUtils.logPlay(getActivity(), true, gameId, gameName);
				return true;
			case R.id.menu_share:
				mode.finish();
				if (mSelectedPositions.size() == 1) {
					ActivityUtils.shareGame(getActivity(), gameId, gameName);
				} else {
					List<Pair<Integer, String>> games = new ArrayList<Pair<Integer, String>>(mSelectedPositions.size());
					for (int position : mSelectedPositions) {
						Cursor c = (Cursor) mAdapter.getItem(position);
						games.add(new Pair<Integer, String>(c.getInt(Query.GAME_ID), c.getString(Query.GAME_NAME)));
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
