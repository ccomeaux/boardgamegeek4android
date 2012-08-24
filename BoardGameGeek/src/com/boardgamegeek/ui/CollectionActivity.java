package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.data.AverageRatingFilterData;
import com.boardgamegeek.data.AverageWeightFilterData;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.CollectionFilterDataFactory;
import com.boardgamegeek.data.CollectionNameSortData;
import com.boardgamegeek.data.CollectionSortData;
import com.boardgamegeek.data.CollectionSortDataFactory;
import com.boardgamegeek.data.CollectionStatusFilterData;
import com.boardgamegeek.data.ExpansionStatusFilterData;
import com.boardgamegeek.data.GeekRankingFilterData;
import com.boardgamegeek.data.GeekRatingFilterData;
import com.boardgamegeek.data.PlayTimeFilterData;
import com.boardgamegeek.data.PlayerNumberFilterData;
import com.boardgamegeek.data.SuggestedAgeFilterData;
import com.boardgamegeek.data.YearPublishedFilterData;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
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
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class CollectionActivity extends ListActivity implements AsyncQueryListener, AbsListView.OnScrollListener {
	private static final String TAG = makeLogTag(CollectionActivity.class);

	private static final String KEY_FILTERS = "FILTERS";
	private static final String KEY_FILTER_NAME = "FILTER_NAME";
	private static final String KEY_FILTER_NAME_PRIOR = "FILTER_NAME_PRIOR";
	private static final String KEY_SORT_TYPE = "SORT";
	private static final int HELP_VERSION = 1;

	private CollectionAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mUri;
	private final BlockingQueue<Uri> mThumbnailQueue = new ArrayBlockingQueue<Uri>(12);
	private ThumbnailTask mThumbnailTask;
	private boolean mShortcut;

	private List<CollectionFilterData> mFilters = new ArrayList<CollectionFilterData>();
	private String mFilterName = "";
	private String mFilterNamePrior = "";
	private CollectionSortData mSort = new CollectionNameSortData(this);

	// Workaround for bug http://code.google.com/p/android/issues/detail?id=7139
	private AdapterContextMenuInfo mLinksMenuInfo = null;

	private TextView mCollectionCountView;
	private TextView mCollectionSortView;
	private TextView mFilterNameView;
	private LinearLayout mFilterLinearLayout;
	// Variables used to manage the appearance of the fast scroll letter.
	private TextView mFastScrollLetter;
	private boolean mFastScrollLetterEnabled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collection);

		UIUtils.setTitle(this);
		mCollectionCountView = (TextView) findViewById(R.id.collection_count);
		mCollectionSortView = (TextView) findViewById(R.id.collection_sort);
		mFilterNameView = (TextView) findViewById(R.id.filter_name);
		mFilterLinearLayout = (LinearLayout) findViewById(R.id.filter_linear_layout);
		mFastScrollLetter = (TextView) findViewById(R.id.fast_scroll_letter);

		getListView().setOnScrollListener(this);
		getListView().setOnCreateContextMenuListener(this);

		if (DateTimeUtils.howManyHoursOld(BggApplication.getInstance().getLastCollectionSync()) > 2) {
			BggApplication.getInstance().putLastCollectionSync();
			startService(new Intent(Intent.ACTION_SYNC, null, this, SyncService.class).putExtra(
				SyncService.KEY_SYNC_TYPE, SyncService.SYNC_TYPE_COLLECTION));
		}

		mAdapter = new CollectionAdapter(this);
		setListAdapter(mAdapter);

		if (getIntent().getAction().equals("android.intent.action.CREATE_SHORTCUT")) {
			mShortcut = true;
			mUri = Collection.CONTENT_URI;
		} else {
			mUri = getIntent().getData();
			UIUtils.allowTypeToSearch(this);
		}
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);

		if (savedInstanceState != null) {
			mFilters = savedInstanceState.getParcelableArrayList(KEY_FILTERS);
			mFilterName = savedInstanceState.getString(KEY_FILTER_NAME);
			mFilterNamePrior = savedInstanceState.getString(KEY_FILTER_NAME_PRIOR);
			mSort = CollectionSortDataFactory.create(savedInstanceState.getInt(KEY_SORT_TYPE), this);
		}

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mFastScrollLetterEnabled = true;
			};
		});

		UIUtils.showHelpDialog(this, BggApplication.HELP_COLLECTION_KEY, HELP_VERSION, R.string.help_collection);
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(Games.CONTENT_URI, true, mGameObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mThumbnailTask = new ThumbnailTask();
		mThumbnailTask.execute();
		mFastScrollLetterEnabled = true;
		getThumbnails(this.getListView());
		bindFilterName();
		requery();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(KEY_FILTERS, (ArrayList<? extends Parcelable>) mFilters);
		outState.putString(KEY_FILTER_NAME, mFilterName);
		outState.putString(KEY_FILTER_NAME_PRIOR, mFilterNamePrior);
		outState.putInt(KEY_SORT_TYPE, mSort == null ? CollectionSortDataFactory.TYPE_UNKNOWN : mSort.getType());
	}

	@Override
	protected void onPause() {
		mThumbnailQueue.clear();
		mThumbnailTask.cancel(true);
		mFastScrollLetter.setVisibility(View.INVISIBLE);
		mFastScrollLetterEnabled = false;
		super.onPause();
	}

	@Override
	protected void onStop() {
		getContentResolver().unregisterContentObserver(mGameObserver);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (mAdapter != null) {
			if (mAdapter.getCursor() != null) {
				mAdapter.getCursor().close();
			}
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mShortcut) {
			return false;
		}
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.collection, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		boolean enabled = false;
		Cursor c = getContentResolver().query(CollectionViews.CONTENT_URI, new String[] { BaseColumns._ID }, null,
			null, null);
		if (c != null) {
			try {
				if (c.getCount() > 0) {
					enabled = true;
				}
			} finally {
				c.close();
			}
		}
		menu.findItem(R.id.menu_collection_view_load).setEnabled(enabled);
		menu.findItem(R.id.menu_collection_view_delete).setEnabled(enabled);

		enabled = mFilters != null && mFilters.size() > 0;
		menu.findItem(R.id.menu_collection_view_save).setEnabled(enabled);
		menu.findItem(R.id.menu_collection_filter_clear).setEnabled(enabled);

		menu.findItem(R.id.menu_collection_random_game).setEnabled(getListAdapter().getCount() > 0);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.menu_collection_random_game:
				final Cursor cursor = (Cursor) mAdapter.getItem(UIUtils.getRandom().nextInt(mAdapter.getCount()));
				showGame(cursor.getInt(Query.GAME_ID));
				return true;
			case R.id.menu_collection_filter_clear:
				mFilters.clear();
				setFilterName("", false);
				requery();
				return true;
			case R.id.menu_collection_view_save:
				SaveFilters.createDialog(this, mFilterNamePrior, mSort.getType(), mFilters);
				return true;
			case R.id.menu_collection_view_load:
				LoadFilters.createDialog(this);
				return true;
			case R.id.menu_collection_view_delete:
				DeleteFilters.createDialog(this);
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
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
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

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return;
		}
		final String gameName = cursor.getString(Query.COLLECTION_NAME);
		UIUtils.createBoardgameContextMenu(menu, menuInfo, gameName);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			if (info == null && mLinksMenuInfo != null) {
				info = mLinksMenuInfo;
			}
		} catch (ClassCastException e) {
			LOGE(TAG, "bad menuInfo", e);
			return false;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return false;
		}

		int gameId = cursor.getInt(Query.GAME_ID);
		String gameName = cursor.getString(Query.COLLECTION_NAME);
		String thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL);

		switch (item.getItemId()) {
			case UIUtils.MENU_ITEM_VIEW: {
				showGame(gameId);
				return true;
			}
			case UIUtils.MENU_ITEM_LOG_PLAY: {
				ActivityUtils.logPlay(this, false, gameId, gameName, thumbnailUrl);
				return true;
			}
			case UIUtils.MENU_ITEM_QUICK_LOG_PLAY: {
				ActivityUtils.logPlay(this, true, gameId, gameName, thumbnailUrl);
				return true;
			}
			case UIUtils.MENU_ITEM_SHARE: {
				ActivityUtils.shareGame(this, gameId, gameName);
				return true;
			}
			case UIUtils.MENU_ITEM_LINKS: {
				mLinksMenuInfo = info;
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_BGG: {
				ActivityUtils.linkBgg(this, gameId);
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_BG_PRICES: {
				ActivityUtils.linkBgPrices(this, gameName);
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_AMAZON: {
				ActivityUtils.linkAmazon(this, gameName);
				return true;
			}
			case UIUtils.MENU_ITEM_LINK_EBAY: {
				ActivityUtils.linkEbay(this, gameName);
				return true;
			}
			case UIUtils.MENU_ITEM_COMMENTS: {
				ActivityUtils.showComments(this, gameId, gameName, thumbnailUrl);
			}
		}
		return false;
	}

	@Override
	public void onContextMenuClosed(Menu menu) {
		// We don't need it anymore
		mLinksMenuInfo = null;
	}

	private boolean launchFilterDialog(int id) {
		switch (id) {
			case R.id.menu_collection_status:
			case CollectionFilterDataFactory.TYPE_COLLECTION_STATUS:
				new CollectionStatusFilter().createDialog(this,
					(CollectionStatusFilterData) findFilter(CollectionFilterDataFactory.TYPE_COLLECTION_STATUS));
				return true;
			case R.id.menu_expansion_status:
			case CollectionFilterDataFactory.TYPE_EXPANSION_STATUS:
				new ExpansionStatusFilter().createDialog(this,
					(ExpansionStatusFilterData) findFilter(CollectionFilterDataFactory.TYPE_EXPANSION_STATUS));
				return true;
			case R.id.menu_number_of_players:
			case CollectionFilterDataFactory.TYPE_PLAYER_NUMBER:
				new PlayerNumberFilter().createDialog(this,
					(PlayerNumberFilterData) findFilter(CollectionFilterDataFactory.TYPE_PLAYER_NUMBER));
				return true;
			case R.id.menu_play_time:
			case CollectionFilterDataFactory.TYPE_PLAY_TIME:
				new PlayTimeFilter().createDialog(this,
					(PlayTimeFilterData) findFilter(CollectionFilterDataFactory.TYPE_PLAY_TIME));
				return true;
			case R.id.menu_suggested_age:
			case CollectionFilterDataFactory.TYPE_SUGGESTED_AGE:
				new SuggestedAgeFilter().createDialog(this,
					(SuggestedAgeFilterData) findFilter(CollectionFilterDataFactory.TYPE_SUGGESTED_AGE));
				return true;
			case R.id.menu_average_weight:
			case CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT:
				new AverageWeightFilter().createDialog(this,
					(AverageWeightFilterData) findFilter(CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT));
				return true;
			case R.id.menu_year_published:
			case CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED:
				new YearPublishedFilter().createDialog(this,
					(YearPublishedFilterData) findFilter(CollectionFilterDataFactory.TYPE_YEAR_PUBLISHED));
				return true;
			case R.id.menu_average_rating:
			case CollectionFilterDataFactory.TYPE_AVERAGE_RATING:
				new AverageRatingFilter().createDialog(this,
					(AverageRatingFilterData) findFilter(CollectionFilterDataFactory.TYPE_AVERAGE_RATING));
				return true;
			case R.id.menu_geek_rating:
			case CollectionFilterDataFactory.TYPE_GEEK_RATING:
				new GeekRatingFilter().createDialog(this,
					(GeekRatingFilterData) findFilter(CollectionFilterDataFactory.TYPE_GEEK_RATING));
				return true;
			case R.id.menu_geek_ranking:
			case CollectionFilterDataFactory.TYPE_GEEK_RANKING:
				new GeekRankingFilter().createDialog(this,
					(GeekRankingFilterData) findFilter(CollectionFilterDataFactory.TYPE_GEEK_RANKING));
				return true;
		}
		return false;
	}

	private void requery() {
		StringBuilder where = new StringBuilder();
		String[] args = {};
		Builder builder = Collection.CONTENT_URI.buildUpon();
		for (CollectionFilterData filter : mFilters) {
			if (!TextUtils.isEmpty(filter.getSelection())) {
				if (where.length() > 0) {
					where.append(" AND ");
				}
				where.append("(").append(filter.getSelection()).append(")");
				args = StringUtils.concat(args, filter.getSelectionArgs());
			}
			if (!TextUtils.isEmpty(filter.getPath())) {
				builder.appendPath(filter.getPath());
			}
		}
		mUri = builder.build();
		mHandler.startQuery(mUri,
			mSort == null ? Query.PROJECTION : StringUtils.unionArrays(Query.PROJECTION, mSort.getColumns()),
			where.toString(), args, mSort == null ? null : mSort.getOrderByClause());
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		UIUtils.showListMessage(this, R.string.empty_collection);
		mAdapter.changeCursor(cursor);
		bindCountAndSortText(cursor);
		bindFilterButtons();
	}

	private void bindCountAndSortText(Cursor cursor) {
		String info = String.format(getResources().getString(R.string.msg_collection_info), cursor.getCount());
		mCollectionCountView.setText(info);
		if (mSort != null) {
			mCollectionSortView.setText("by " + getResources().getString(mSort.getDescriptionId()));
		}
	}

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
		final Button button = new Button(this);
		button.setText(text);
		button.setTag(type);
		button.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_tiny));
		button.setLongClickable(true);
		button.setBackgroundResource(R.drawable.button_filter_normal);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT);
		int margin = (int) getResources().getDimension(R.dimen.padding_small);
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

	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		if (mShortcut) {
			Intent shortcut = ActivityUtils.createShortcut(this, cursor.getInt(Query.GAME_ID),
				cursor.getString(Query.COLLECTION_NAME), cursor.getString(Query.THUMBNAIL_URL));
			setResult(RESULT_OK, shortcut);
			finish();
		} else {
			showGame(cursor.getInt(Query.GAME_ID));
		}
	}

	private void showGame(final int gameId) {
		final Uri gameUri = Games.buildGameUri(gameId);
		startActivity(new Intent(Intent.ACTION_VIEW, gameUri));
	}

	private ContentObserver mGameObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			requery();
		}
	};

	private class CollectionAdapter extends CursorAdapter {
		private LayoutInflater mInflater;
		String mUnknownYear = getResources().getString(R.string.text_unknown);

		public CollectionAdapter(Context context) {
			super(context, null);
			mInflater = getLayoutInflater();
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
			holder.name.setText(cursor.getString(Query.COLLECTION_NAME));

			int year = cursor.getInt(Query.YEAR_PUBLISHED);
			holder.year.setText((year > 0) ? String.valueOf(year) : mUnknownYear);
			holder.info.setText(mSort == null ? "" : mSort.getDisplayInfo(cursor));
			holder.thumbnailUrl = Collection.buildThumbnailUri(cursor.getInt(Query.COLLECTION_ID));

			Drawable thumbnail = ImageCache.getCollectionThumbnailFromCache(CollectionActivity.this,
				holder.thumbnailUrl);

			if (thumbnail == null) {
				holder.thumbnail.setVisibility(View.GONE);
			} else {
				holder.thumbnailUrl = null;
				holder.thumbnail.setImageDrawable(thumbnail);
				holder.thumbnail.setVisibility(View.VISIBLE);
			}
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

	private class ThumbnailTask extends AsyncTask<Void, Void, Void> {

		private ListView mView;

		@Override
		protected void onPreExecute() {
			mView = CollectionActivity.this.getListView();
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled()) {
				try {
					Uri uri = mThumbnailQueue.take();
					ImageCache.getCollectionThumbnail(CollectionActivity.this, uri);
					publishProgress();
				} catch (InterruptedException e) {
					LOGE(TAG, "getting image from cache", e);
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			mView.invalidateViews();
		}
	}

	private interface Query {
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

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (mFastScrollLetterEnabled && mAdapter != null) {
			final Cursor cursor = (Cursor) mAdapter.getItem(firstVisibleItem);
			if (cursor != null && cursor.getCount() > 0) {
				mFastScrollLetter.setText(mSort == null ? "" : mSort.getScrollText(cursor));
			}
		}
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE) {
			mFastScrollLetter.setVisibility(View.INVISIBLE);
			getThumbnails(view);
		} else {
			mFastScrollLetter.setVisibility(View.VISIBLE);
			mThumbnailQueue.clear();
		}
	}

	private void getThumbnails(AbsListView view) {
		final int count = view.getChildCount();
		for (int i = 0; i < count; i++) {
			ViewHolder vh = (ViewHolder) view.getChildAt(i).getTag();
			if (vh.thumbnailUrl != null) {
				mThumbnailQueue.offer(vh.thumbnailUrl);
			}
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

	public void removeFilter(CollectionFilterData filter) {
		setFilterName("", true);
		mFilters.remove(filter);
		requery();
	}

	public void addFilter(CollectionFilterData filter) {
		setFilterName("", true);
		mFilters.remove(filter);
		if (filter.isValid()) {
			mFilters.add(filter);
		}
		requery();
	}

	public void setFilters(List<CollectionFilterData> filters) {
		mFilters = filters;
		requery();
	}

	public void setFilterName(String name, boolean saveName) {
		if (saveName && TextUtils.isEmpty(name) && !TextUtils.isEmpty(mFilterName)) {
			mFilterNamePrior = mFilterName;
		} else if (!saveName) {
			mFilterNamePrior = "";
		}
		mFilterName = name;
		bindFilterName();
	}

	public void setSort(int sortType) {
		setFilterName("", true);
		if (sortType == CollectionSortDataFactory.TYPE_UNKNOWN) {
			sortType = CollectionSortDataFactory.TYPE_DEFAULT;
		}
		mSort = CollectionSortDataFactory.create(sortType, this);
		requery();
	}
}