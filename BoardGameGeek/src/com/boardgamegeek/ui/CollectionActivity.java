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
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilter;
import com.boardgamegeek.data.PlayerNumberFilter;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.dialog.CollectionStatusFilter;
import com.boardgamegeek.ui.dialog.NumberOfPlayersFilter;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class CollectionActivity extends ListActivity implements AsyncQueryListener, AbsListView.OnScrollListener {
	private static final String TAG = "CollectionActivity";

	private CollectionAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mUri;
	private final BlockingQueue<String> mThumbnailQueue = new ArrayBlockingQueue<String>(12);
	private ThumbnailTask mThumbnailTask;
	private TextView mInfoView;

	private LinearLayout mFilterLinearLayout;
	private List<CollectionFilter> mFilters = new ArrayList<CollectionFilter>();

	private NumberOfPlayersFilter mNumberOfPlayersFilter = new NumberOfPlayersFilter();
	private CollectionStatusFilter mCollectionStatusFilter = new CollectionStatusFilter();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collection);

		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);
		mInfoView = (TextView) findViewById(R.id.collection_info);
		mFilterLinearLayout = (LinearLayout) findViewById(R.id.filterLinearLayout);

		getListView().setOnScrollListener(this);

		mAdapter = new CollectionAdapter(this);
		setListAdapter(mAdapter);

		mUri = getIntent().getData();
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		applyFilters();
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mUri, true, mGameObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mThumbnailTask = new ThumbnailTask();
		mThumbnailTask.execute();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mThumbnailQueue.clear();
		mThumbnailTask.cancel(true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		getContentResolver().unregisterContentObserver(mGameObserver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.collection, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem mi = menu.findItem(R.id.menu_collection_filter_clear);
		mi.setEnabled(mFilters != null && mFilters.size() > 0);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.menu_collection_random_game:
				final Cursor cursor = (Cursor) mAdapter.getItem(UIUtils.getRandom().nextInt(mAdapter.getCount()));
				final int gameId = cursor.getInt(Query.GAME_ID);
				final Uri gameUri = Games.buildGameUri(gameId);
				startActivity(new Intent(Intent.ACTION_VIEW, gameUri));
				return true;
			case R.id.menu_collection_filter_clear:
				mFilters.clear();
				applyFilters();
				return true;
		}

		if (launchFilterDialog(item.getItemId())) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private boolean launchFilterDialog(int id) {
		switch (id) {
			case R.id.menu_number_of_players:
				PlayerNumberFilter filter = (PlayerNumberFilter) findFilter(id);
				mNumberOfPlayersFilter.createDialog(this, filter);
				return true;
			case R.id.menu_collection_status:
				mCollectionStatusFilter.createDialog(this);
				return true;
		}
		return false;
	}

	private void applyFilters() {
		StringBuilder where = new StringBuilder();
		String[] args = {};

		for (CollectionFilter filter : mFilters) {
			if (!TextUtils.isEmpty(filter.getSelection())) {
				if (where.length() > 0) {
					where.append(" AND ");
				}
				where.append("(").append(filter.getSelection()).append(")");
				args = StringUtils.concat(args, filter.getSelectionArgs());
			}
		}

		mHandler.startQuery(mUri, Query.PROJECTION, where.toString(), args, Collection.DEFAULT_SORT);
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
		startManagingCursor(cursor);
		mAdapter.changeCursor(cursor);

		if (cursor != null) {
			mInfoView.setText(String.format(getResources().getString(R.string.msg_collection_info), cursor.getCount()));
		}

		syncFilterButtons();
	}

	private void syncFilterButtons() {
		for (CollectionFilter filter : mFilters) {
			Button button = (Button) mFilterLinearLayout.findViewById(filter.getId());
			if (button == null) {
				mFilterLinearLayout.addView(createFilterButton(filter.getId(), filter.getDisplayText()));
			} else {
				button.setText(filter.getDisplayText());
			}
		}

		// Could be when button is clicked, but this keeps filters synced with
		// collection
		for (int i = 0; i < mFilterLinearLayout.getChildCount(); i++) {
			Button button = (Button) mFilterLinearLayout.getChildAt(i);
			if (!mFilters.contains(new CollectionFilter().id(button.getId()))) {
				mFilterLinearLayout.removeView(button);
				i--;
			}
		}
	}

	private Button createFilterButton(final int id, String text) {
		final Button button = new Button(this);
		button.setId(id);
		button.setText(text);
		button.setTextSize(18); // TODO: put in style
		button.setLongClickable(true);
		button.setBackgroundResource(R.drawable.button_filter_normal);
		button.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchFilterDialog(v.getId());
			}
		});
		button.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				mFilters.remove(new CollectionFilter().id(id));
				applyFilters();
				return true;
			}
		});
		return button;
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int gameId = cursor.getInt(Query.GAME_ID);
		final Uri gameUri = Games.buildGameUri(gameId);
		startActivity(new Intent(Intent.ACTION_VIEW, gameUri));
	}

	private ContentObserver mGameObserver = new ContentObserver(new Handler()) {
		private static final long OBSERVER_THROTTLE_IN_MILLIS = 10000; // 10s

		private long mLastUpdated;

		@Override
		public void onChange(boolean selfChange) {
			long now = System.currentTimeMillis();
			if (now - mLastUpdated > OBSERVER_THROTTLE_IN_MILLIS) {
				applyFilters();
				mLastUpdated = System.currentTimeMillis();
			}
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
			thumbnail = (BezelImageView) view.findViewById(R.id.listThumbnail);
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
				Collection.YEAR_PUBLISHED, Games.GAME_NAME, Games.GAME_ID, Games.THUMBNAIL_URL, };

		// int _ID = 0;
		// int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		int YEAR_PUBLISHED = 3;
		// int GAME_NAME = 4;
		int GAME_ID = 5;
		int THUMBNAIL_URL = 6;
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// do nothing
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE) {
			getThumbnails(view);
		} else {
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

	private CollectionFilter findFilter(int id) {
		for (CollectionFilter filter : mFilters) {
			if (filter.getId() == id) {
				return filter;
			}
		}
		return null;
	}

	public void removeFilter(CollectionFilter filter) {
		mFilters.remove(filter);
		applyFilters();
	}

	public void addFilter(CollectionFilter filter) {
		mFilters.remove(filter);
		mFilters.add(filter);
		applyFilters();
	}
}
