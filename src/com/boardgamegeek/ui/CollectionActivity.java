package com.boardgamegeek.ui;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
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
import com.boardgamegeek.data.AverageWeightFilterData;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.CollectionFilterDataFactory;
import com.boardgamegeek.data.CollectionStatusFilterData;
import com.boardgamegeek.data.PlayTimeFilterData;
import com.boardgamegeek.data.PlayerNumberFilterData;
import com.boardgamegeek.data.SuggestedAgeFilterData;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.dialog.AverageWeightFilter;
import com.boardgamegeek.ui.dialog.CollectionStatusFilter;
import com.boardgamegeek.ui.dialog.DeleteFilters;
import com.boardgamegeek.ui.dialog.LoadFilters;
import com.boardgamegeek.ui.dialog.PlayTimeFilter;
import com.boardgamegeek.ui.dialog.PlayerNumberFilter;
import com.boardgamegeek.ui.dialog.SaveFilters;
import com.boardgamegeek.ui.dialog.SuggestedAgeFilter;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class CollectionActivity extends ListActivity implements AsyncQueryListener, AbsListView.OnScrollListener {
	private static final String TAG = "CollectionActivity";

	private static final String KEY_FILTERS = "FILTERS";
	private static final String KEY_FILTER_NAME = "FILTER_NAME";
	private static final String KEY_FILTER_NAME_PRIOR = "FILTER_NAME_PRIOR";
	private static final int HELP_VERSION = 1;

	private CollectionAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mUri;
	private final BlockingQueue<String> mThumbnailQueue = new ArrayBlockingQueue<String>(12);
	private ThumbnailTask mThumbnailTask;
	private TextView mInfoView;
	private boolean mShortcut;

	private LinearLayout mFilterLinearLayout;
	private List<CollectionFilterData> mFilters = new ArrayList<CollectionFilterData>();
	private String mFilterName = "";
	private String mFilterNamePrior = "";
	private CollectionStatusFilter mCollectionStatusFilter = new CollectionStatusFilter();
	private PlayerNumberFilter mNumberOfPlayersFilter = new PlayerNumberFilter();
	private PlayTimeFilter mPlayTimeFilter = new PlayTimeFilter();
	private SuggestedAgeFilter mSuggestedAgeFilter = new SuggestedAgeFilter();
	private AverageWeightFilter mAverageWeightFilter = new AverageWeightFilter();

	// Workaround for bug http://code.google.com/p/android/issues/detail?id=7139
	private AdapterContextMenuInfo mLinksMenuInfo = null;

	// Variables used to manage the appearance of the fast scroll letter.
	private TextView mFastScrollLetter;
	private boolean mFastScrollLetterEnabled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collection);

		UIUtils.setTitle(this);
		mInfoView = (TextView) findViewById(R.id.collection_info);
		mFilterLinearLayout = (LinearLayout) findViewById(R.id.filterLinearLayout);
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
		}
		applyFilters();

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
		getContentResolver().registerContentObserver(mUri, false, mGameObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mThumbnailTask = new ThumbnailTask();
		mThumbnailTask.execute();
		mFastScrollLetterEnabled = true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(KEY_FILTERS, (ArrayList<? extends Parcelable>) mFilters);
		outState.putString(KEY_FILTER_NAME, mFilterName);
		outState.putString(KEY_FILTER_NAME_PRIOR, mFilterNamePrior);
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
		Cursor c = getContentResolver().query(CollectionFilters.CONTENT_URI, new String[] { BaseColumns._ID }, null,
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
		menu.findItem(R.id.menu_collection_filter_load).setEnabled(enabled);
		menu.findItem(R.id.menu_collection_filter_delete).setEnabled(enabled);

		enabled = mFilters != null && mFilters.size() > 0;
		menu.findItem(R.id.menu_collection_filter_save).setEnabled(enabled);
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
				setFilterName("");
				applyFilters();
				return true;
			case R.id.menu_collection_filter_save:
				SaveFilters.createDialog(this, mFilterNamePrior, mFilters);
				return true;
			case R.id.menu_collection_filter_load:
				LoadFilters.createDialog(this);
				return true;
			case R.id.menu_collection_filter_delete:
				DeleteFilters.createDialog(this);
				return true;
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
			Log.e(TAG, "bad menuInfo", e);
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
			Log.e(TAG, "bad menuInfo", e);
			return false;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return false;
		}

		final int gameId = cursor.getInt(Query.GAME_ID);
		final String gameName = cursor.getString(Query.COLLECTION_NAME);
		final String thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL);
		Log.d(TAG, gameName);

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
				mCollectionStatusFilter.createDialog(this,
						(CollectionStatusFilterData) findFilter(CollectionFilterDataFactory.TYPE_COLLECTION_STATUS));
				return true;
			case R.id.menu_number_of_players:
			case CollectionFilterDataFactory.TYPE_PLAYER_NUMBER:
				mNumberOfPlayersFilter.createDialog(this,
						(PlayerNumberFilterData) findFilter(CollectionFilterDataFactory.TYPE_PLAYER_NUMBER));
				return true;
			case R.id.menu_play_time:
			case CollectionFilterDataFactory.TYPE_PLAY_TIME:
				mPlayTimeFilter.createDialog(this,
						(PlayTimeFilterData) findFilter(CollectionFilterDataFactory.TYPE_PLAY_TIME));
				return true;
			case R.id.menu_suggested_age:
			case CollectionFilterDataFactory.TYPE_SUGGESTED_AGE:
				mSuggestedAgeFilter.createDialog(this,
						(SuggestedAgeFilterData) findFilter(CollectionFilterDataFactory.TYPE_SUGGESTED_AGE));
				return true;
			case R.id.menu_average_weight:
			case CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT:
				mAverageWeightFilter.createDialog(this,
						(AverageWeightFilterData) findFilter(CollectionFilterDataFactory.TYPE_AVERAGE_WEIGHT));
				return true;
		}
		return false;
	}

	private void applyFilters() {
		StringBuilder where = new StringBuilder();
		String[] args = {};

		for (CollectionFilterData filter : mFilters) {
			if (!TextUtils.isEmpty(filter.getSelection())) {
				if (where.length() > 0) {
					where.append(" AND ");
				}
				where.append("(").append(filter.getSelection()).append(")");
				args = StringUtils.concat(args, filter.getSelectionArgs());
			}
		}

		mHandler.startQuery(mUri, Query.PROJECTION, where.toString(), args, null);
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
		setInfoText(cursor);
		syncFilterButtons();
	}

	private void setInfoText(Cursor cursor) {
		final String delimiter = "   -   ";
		String info = mFilterName;
		if (!TextUtils.isEmpty(info)) {
			info += delimiter;
		}
		if (cursor != null) {
			info += String.format(getResources().getString(R.string.msg_collection_info), cursor.getCount());
		} else {
			String[] s = mInfoView.getText().toString().split(delimiter);
			info += (s.length == 1) ? s[0] : s[1];
		}
		mInfoView.setText(info);
	}

	private void syncFilterButtons() {
		for (CollectionFilterData filter : mFilters) {
			Button button = (Button) mFilterLinearLayout.findViewById(filter.getType());
			if (button == null) {
				mFilterLinearLayout.addView(createFilterButton(filter.getType(), filter.getDisplayText()));
			} else {
				button.setText(filter.getDisplayText());
			}
		}

		// Could be when button is clicked, but this keeps filters synced with collection
		for (int i = 0; i < mFilterLinearLayout.getChildCount(); i++) {
			Button button = (Button) mFilterLinearLayout.getChildAt(i);
			if (!mFilters.contains(new CollectionFilterData(button.getId()))) {
				mFilterLinearLayout.removeView(button);
				i--;
			}
		}
	}

	private Button createFilterButton(final int type, String text) {
		final Button button = new Button(this);
		button.setId(type);
		button.setText(text);
		button.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_small));
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
				launchFilterDialog(v.getId());
			}
		});
		button.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				mFilters.remove(new CollectionFilterData(type));
				applyFilters();
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
			applyFilters();
		}
	};

	private class CollectionAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

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

			String yearPublished = getResources().getString(R.string.text_unknown);
			int year = cursor.getInt(Query.YEAR_PUBLISHED);
			if (year > 0) {
				yearPublished = "" + year;
			}
			holder.year.setText(yearPublished);
			holder.thumbnailUrl = cursor.getString(Query.THUMBNAIL_URL);

			Drawable thumbnail = ImageCache.getDrawableFromCache(holder.thumbnailUrl);

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
		BezelImageView thumbnail;
		String thumbnailUrl;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			thumbnail = (BezelImageView) view.findViewById(R.id.list_thumbnail);
		}
	}

	private class ThumbnailTask extends AsyncTask<Void, String, Void> {

		private ListView mView;

		@Override
		protected void onPreExecute() {
			mView = CollectionActivity.this.getListView();
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled()) {
				try {
					String url = mThumbnailQueue.take();
					ImageCache.getImage(CollectionActivity.this, url);
					publishProgress(url);
				} catch (InterruptedException e) {
					Log.w(TAG, e.toString());
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			mView.invalidateViews();
		}
	}

	private interface Query {
		String[] PROJECTION = { BaseColumns._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME,
				Collection.YEAR_PUBLISHED, Games.GAME_NAME, Games.GAME_ID, Games.THUMBNAIL_URL,
				Collection.COLLECTION_SORT_NAME };

		// int _ID = 0;
		// int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		int YEAR_PUBLISHED = 3;
		// int GAME_NAME = 4;
		int GAME_ID = 5;
		int THUMBNAIL_URL = 6;
		int COLLECTION_SORT_NAME = 7;
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (mFastScrollLetterEnabled && mAdapter != null) {
			final Cursor cursor = (Cursor) mAdapter.getItem(firstVisibleItem);
			if (cursor != null && cursor.getCount() > 0) {
				char firstLetter = cursor.getString(Query.COLLECTION_SORT_NAME).toUpperCase().charAt(0);
				mFastScrollLetter.setText(((Character) firstLetter).toString());
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
		setFilterName("");
		mFilters.remove(filter);
		applyFilters();
	}

	public void addFilter(CollectionFilterData filter) {
		setFilterName("");
		mFilters.remove(filter);
		if (filter.isValid()) {
			mFilters.add(filter);
		}
		applyFilters();
	}

	public void setFilters(List<CollectionFilterData> filters) {
		mFilters = filters;
		applyFilters();
	}

	public void setFilterName(String name) {
		mFilterNamePrior = mFilterName;
		mFilterName = name;
		setInfoText(null);
	}
}