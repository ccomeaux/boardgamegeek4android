package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
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
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.dialog.AverageRatingFilter;
import com.boardgamegeek.ui.dialog.AverageWeightFilter;
import com.boardgamegeek.ui.dialog.CollectionStatusFilter;
import com.boardgamegeek.ui.dialog.DeleteFilters;
import com.boardgamegeek.ui.dialog.ExpansionStatusFilter;
import com.boardgamegeek.ui.dialog.GeekRankingFilter;
import com.boardgamegeek.ui.dialog.GeekRatingFilter;
import com.boardgamegeek.ui.dialog.LoadFilters;
import com.boardgamegeek.ui.dialog.PlayTimeFilter;
import com.boardgamegeek.ui.dialog.PlayerNumberFilter;
import com.boardgamegeek.ui.dialog.SaveFilters;
import com.boardgamegeek.ui.dialog.SuggestedAgeFilter;
import com.boardgamegeek.ui.dialog.YearPublishedFilter;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageFetcher;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class CollectionFragment extends SherlockListFragment implements AbsListView.OnScrollListener,
	LoaderManager.LoaderCallbacks<Cursor>, CollectionView {
	private static final String TAG = makeLogTag(CollectionFragment.class);
	private static final String KEY_FILTERS = "FILTERS";
	private static final String KEY_FILTER_NAME = "FILTER_NAME";
	private static final String KEY_FILTER_NAME_PRIOR = "FILTER_NAME_PRIOR";
	private static final String KEY_SORT_TYPE = "SORT";
	private static final String STATE_SELECTED_ID = "selectedId";

	private ImageFetcher mImageFetcher;
	private int mSelectedCollectionId;
	private boolean mFastScrollLetterEnabled;
	private CollectionAdapter mAdapter;
	private CollectionSortData mSort;
	private List<CollectionFilterData> mFilters = new ArrayList<CollectionFilterData>();
	private String mFilterName = "";
	private String mFilterNamePrior = "";

	private View mProgressView;
	private View mListContainer;
	private TextView mCollectionCountView;
	private TextView mCollectionSortView;
	private TextView mFilterNameView;
	private LinearLayout mFilterLinearLayout;
	private TextView mFastScrollLetter;
	private boolean mShortcut;

	public interface Callbacks {
		public boolean onGameSelected(int gameId, String gameName);
		public void onSetShortcut(Intent intent);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public boolean onGameSelected(int gameId, String gameName) {
			return true;
		}

		@Override
		public void onSetShortcut(Intent intent) {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mImageFetcher = UIUtils.getImageFetcher(getActivity());
		mImageFetcher.setLoadingImage(R.drawable.person_image_empty);
		mImageFetcher.setImageSize((int) getResources().getDimension(R.dimen.thumbnail_list_size));

		if (savedInstanceState != null) {
			mSelectedCollectionId = savedInstanceState.getInt(STATE_SELECTED_ID);
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_collection, null);

		mProgressView = rootView.findViewById(R.id.progress);
		mListContainer = rootView.findViewById(R.id.list_container);
		mCollectionCountView = (TextView) rootView.findViewById(R.id.collection_count);
		mCollectionSortView = (TextView) rootView.findViewById(R.id.collection_sort);
		mFilterNameView = (TextView) rootView.findViewById(R.id.filter_name);
		mFilterLinearLayout = (LinearLayout) rootView.findViewById(R.id.filter_linear_layout);
		mFastScrollLetter = (TextView) rootView.findViewById(R.id.fast_scroll_letter);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setCacheColorHint(Color.WHITE);
		listView.setFastScrollEnabled(true);
		listView.setOnScrollListener(this);
		registerForContextMenu(listView);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mSort = new CollectionNameSortData(getActivity());
		if (savedInstanceState != null) {
			mSelectedCollectionId = savedInstanceState.getInt(STATE_SELECTED_ID);
			mFilters = savedInstanceState.getParcelableArrayList(KEY_FILTERS);
			mFilterName = savedInstanceState.getString(KEY_FILTER_NAME);
			mFilterNamePrior = savedInstanceState.getString(KEY_FILTER_NAME_PRIOR);
			mSort = CollectionSortDataFactory.create(savedInstanceState.getInt(KEY_SORT_TYPE), getActivity());
		}
		requery();
		bindFilterName();
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
		outState.putParcelableArrayList(KEY_FILTERS, (ArrayList<? extends Parcelable>) mFilters);
		outState.putString(KEY_FILTER_NAME, mFilterName);
		outState.putString(KEY_FILTER_NAME_PRIOR, mFilterNamePrior);
		outState.putInt(KEY_SORT_TYPE, mSort == null ? CollectionSortDataFactory.TYPE_UNKNOWN : mSort.getType());
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
		mImageFetcher.flushCache();
		mFastScrollLetter.setVisibility(View.INVISIBLE);
		mFastScrollLetterEnabled = false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mImageFetcher.closeCache();
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
		inflater.inflate(R.menu.collection, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean hasViews = ResolverUtils.getCount(getActivity().getContentResolver(), CollectionViews.CONTENT_URI) > 0;
		menu.findItem(R.id.menu_collection_view_load).setEnabled(hasViews);
		menu.findItem(R.id.menu_collection_view_delete).setEnabled(hasViews);

		boolean hasFilters = mFilters != null && mFilters.size() > 0;
		menu.findItem(R.id.menu_collection_view_save).setEnabled(hasFilters);
		menu.findItem(R.id.menu_collection_filter_clear).setEnabled(hasFilters);

		final MenuItem item = menu.findItem(R.id.menu_collection_random_game);
		item.setVisible(!mShortcut);
		item.setEnabled(mAdapter == null ? false : mAdapter.getCount() > 0);
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
			case R.id.menu_collection_filter_clear:
				mFilters.clear();
				setFilterName("", false);
				getLoaderManager().restartLoader(Query._TOKEN, null, this);
				return true;
			case R.id.menu_collection_view_save:
				SaveFilters.createDialog(getActivity(), this, mFilterNamePrior, mSort.getType(), mFilters);
				return true;
			case R.id.menu_collection_view_load:
				LoadFilters.createDialog(getActivity(), this);
				return true;
			case R.id.menu_collection_view_delete:
				DeleteFilters.createDialog(getActivity());
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
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (mShortcut) {
			return;
		}

		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) mAdapter.getItem(info.position);
		if (cursor == null) {
			return;
		}
		final String gameName = cursor.getString(Query.COLLECTION_NAME);
		UIUtils.createBoardgameContextMenu(menu, menuInfo, gameName);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return false;
		}

		Cursor cursor = (Cursor) mAdapter.getItem(info.position);
		if (cursor == null) {
			return false;
		}

		int gameId = cursor.getInt(Query.GAME_ID);
		String gameName = cursor.getString(Query.COLLECTION_NAME);

		switch (item.getItemId()) {
			case UIUtils.MENU_ITEM_VIEW:
				ActivityUtils.launchGame(getActivity(), gameId, gameName);
				return true;
			case UIUtils.MENU_ITEM_LOG_PLAY: {
				ActivityUtils.logPlay(getActivity(), false, gameId, gameName);
				return true;
			}
			case UIUtils.MENU_ITEM_QUICK_LOG_PLAY: {
				ActivityUtils.logPlay(getActivity(), true, gameId, gameName);
				return true;
			}
			case UIUtils.MENU_ITEM_SHARE: {
				ActivityUtils.shareGame(getActivity(), gameId, gameName);
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_BGG: {
				ActivityUtils.linkBgg(getActivity(), gameId);
				return true;
			}
		}
		return super.onContextItemSelected(item);
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
		// Pause disk cache access to ensure smoother scrolling
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
			|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			mImageFetcher.setPauseWork(true);
		} else {
			mImageFetcher.setPauseWork(false);
		}

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

		int token = loader.getId();
		if (token == Query._TOKEN) {
			mAdapter.changeCursor(cursor);
			bindCountAndSortText(cursor);
			bindFilterButtons();
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}

		if (mListContainer.getVisibility() != View.VISIBLE) {
			if (isResumed()) {
				mProgressView.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
				mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
			} else {
				mProgressView.clearAnimation();
				mListContainer.clearAnimation();
			}
			mProgressView.setVisibility(View.GONE);
			mListContainer.setVisibility(View.VISIBLE);
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
	public void setSort(int sortType) {
		setFilterName("", true);
		if (sortType == CollectionSortDataFactory.TYPE_UNKNOWN) {
			sortType = CollectionSortDataFactory.TYPE_DEFAULT;
		}
		mSort = CollectionSortDataFactory.create(sortType, getActivity());
		requery();
	}

	@Override
	public void setFilterName(String name, boolean saveName) {
		if (saveName && TextUtils.isEmpty(name) && !TextUtils.isEmpty(mFilterName)) {
			mFilterNamePrior = mFilterName;
		} else if (!saveName) {
			mFilterNamePrior = "";
		}
		mFilterName = name;
		bindFilterName();
	}

	/**
	 * Display the name of the filter in the UI
	 */
	private void bindFilterName() {
		if (TextUtils.isEmpty(mFilterName)) {
			if (TextUtils.isEmpty(mFilterNamePrior)) {
				mFilterNameView.setText("");
			} else {
				mFilterNameView.setText(mFilterNamePrior + "*");
			}
		} else {
			mFilterNameView.setText(mFilterName);
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

	@Override
	public void removeFilter(CollectionFilterData filter) {
		setFilterName("", true);
		mFilters.remove(filter);
		requery();
	}

	@Override
	public void addFilter(CollectionFilterData filter) {
		setFilterName("", true);
		mFilters.remove(filter);
		if (filter.isValid()) {
			mFilters.add(filter);
		}
		requery();
	}

	@Override
	public void setFilters(List<CollectionFilterData> filters) {
		mFilters = filters;
		requery();
	}

	private void bindCountAndSortText(Cursor cursor) {
		String info = String.format(getResources().getString(R.string.msg_collection_info), cursor.getCount());
		mCollectionCountView.setText(info);
		if (mSort != null) {
			mCollectionSortView.setText("by " + getResources().getString(mSort.getDescriptionId()));
		}
	}

	private void bindFilterButtons() {
		for (CollectionFilterData filter : mFilters) {
			Button button = (Button) mFilterLinearLayout.findViewWithTag(filter.getType());
			if (button == null) {
				mFilterLinearLayout.addView(createFilterButton(filter.getType(), filter.getDisplayText()));
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

	private Button createFilterButton(final int type, String text) {
		final Button button = new Button(getActivity());
		button.setText(text);
		button.setTag(type);
		button.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_tiny));
		button.setLongClickable(true);
		button.setBackgroundResource(R.drawable.button_filter_normal);
		// TODO: figure out why there's so much padding in JB compared to API8
		// int padding = getResources().getDimensionPixelSize(R.dimen.padding_small);
		// button.setPadding(padding, padding, padding, padding);
		// button.setMinHeight(1);
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

	private class CollectionAdapter extends CursorAdapter {
		private LayoutInflater mInflater;
		String mUnknownYear = getResources().getString(R.string.text_unknown);

		public CollectionAdapter(Context context) {
			super(context, null, false);
			mInflater = getActivity().getLayoutInflater();
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

			int collectionId = cursor.getInt(Query.COLLECTION_ID);
			int year = cursor.getInt(Query.YEAR_PUBLISHED);
			String thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL);

			UIUtils.setActivatedCompat(view, collectionId == mSelectedCollectionId);

			holder.name.setText(cursor.getString(Query.COLLECTION_NAME));
			holder.year.setText((year > 0) ? String.valueOf(year) : mUnknownYear);
			holder.info.setText(mSort == null ? "" : mSort.getDisplayInfo(cursor));
			mImageFetcher.loadThumnailImage(thumbnailUrl, Collection.buildThumbnailUri(collectionId), holder.thumbnail);
		}
	}

	static class ViewHolder {
		TextView name;
		TextView year;
		TextView info;
		BezelImageView thumbnail;
		Uri thumbnailUrl;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			info = (TextView) view.findViewById(R.id.info);
			thumbnail = (BezelImageView) view.findViewById(R.id.list_thumbnail);
		}
	}

	private interface Query {
		int _TOKEN = 0x01;
		String[] PROJECTION = { BaseColumns._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME,
			Collection.YEAR_PUBLISHED, Games.GAME_NAME, Games.GAME_ID, Collection.THUMBNAIL_URL };

		// int _ID = 0;
		int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		int YEAR_PUBLISHED = 3;
		// int GAME_NAME = 4;
		int GAME_ID = 5;
		int THUMBNAIL_URL = 6;
	}
}
