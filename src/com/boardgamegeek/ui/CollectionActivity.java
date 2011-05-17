package com.boardgamegeek.ui;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class CollectionActivity extends ListActivity implements AsyncQueryListener, AbsListView.OnScrollListener {
	private static final String TAG = "CollectionActivity";

	private static final long OBSERVER_THROTTLE_IN_MILLIS = 10000; // 10 seconds

	private CollectionAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mUri;
	private CollectionObserver mGameObserver;
	private long mLastUpdated;
	private final BlockingQueue<String> mThumbnailQueue = new ArrayBlockingQueue<String>(12);
	private ThumbnailTask mThumbnailTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collection);

		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		getListView().setOnScrollListener(this);

		mGameObserver = new CollectionObserver(null);
		mAdapter = new CollectionAdapter(this);
		setListAdapter(mAdapter);

		mUri = getIntent().getData();
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mUri, Query.PROJECTION, null, null, Collection.DEFAULT_SORT);
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
			TextView tv = (TextView) findViewById(R.id.collection_info);
			String message = String.format(getResources().getString(R.string.msg_collection_info), cursor.getCount());
			tv.setText(message);
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int gameId = cursor.getInt(Query.GAME_ID);
		final Uri gameUri = Games.buildGameUri(gameId);
		startActivity(new Intent(Intent.ACTION_VIEW, gameUri));
	}

	class CollectionObserver extends ContentObserver {

		public CollectionObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			long now = System.currentTimeMillis();
			if (now - mLastUpdated > OBSERVER_THROTTLE_IN_MILLIS) {
				mHandler.startQuery(mUri, Query.PROJECTION, null, null, Collection.DEFAULT_SORT);
				mLastUpdated = System.currentTimeMillis();
			}
		}
	}

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
			final int count = view.getChildCount();
			for (int i = 0; i < count; i++) {
				ViewHolder vh = (ViewHolder) view.getChildAt(i).getTag();
				if (vh.thumbnailUrl != null) {
					mThumbnailQueue.offer(vh.thumbnailUrl);
				}
			}
		} else {
			mThumbnailQueue.clear();
		}
	}
}
