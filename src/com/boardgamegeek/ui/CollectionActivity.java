package com.boardgamegeek.ui;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class CollectionActivity extends ListActivity implements
		AsyncQueryListener {

	private CollectionAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collection);

		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		mAdapter = new CollectionAdapter(this);
		setListAdapter(mAdapter);

		Uri uri = getIntent().getData();
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(uri, CollectionQuery.PROJECTION, null, null,
				Collection.DEFAULT_SORT);
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
		changeEmptyMessage();
		startManagingCursor(cursor);
		mAdapter.changeCursor(cursor);
	}

	private void changeEmptyMessage() {
		TextView tv = (TextView) findViewById(R.id.listMessage);
		tv.setText(R.string.empty_collection);

		ProgressBar pb = (ProgressBar) findViewById(R.id.listProgress);
		pb.setVisibility(View.GONE);
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int gameId = cursor.getInt(CollectionQuery.GAME_ID);
		final Uri gameUri = Games.buildGameUri(gameId);
		startActivity(new Intent(Intent.ACTION_VIEW, gameUri));
	}

	private class CollectionAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public CollectionAdapter(Context context) {
			super(context, null);
			mInflater = getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.collection_row, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.name.setText(cursor
					.getString(CollectionQuery.COLLECTION_NAME));
			holder.year.setText(cursor
					.getString(CollectionQuery.YEAR_PUBLISHED));
			Drawable thumbnail = ImageCache.getDrawableFromCache(cursor
					.getString(CollectionQuery.THUMBNAIL_URL));
			
			if (thumbnail == null) {
				holder.thumbnail.setVisibility(View.GONE);
			} else {
				holder.thumbnail.setVisibility(View.VISIBLE);
				holder.thumbnail.setImageDrawable(thumbnail);
			}
		}
	}

	static class ViewHolder {
		TextView name;
		TextView year;
		ImageView thumbnail;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			thumbnail = (ImageView) view.findViewById(R.id.listThumbnail);
		}
	}

	private interface CollectionQuery {
		String[] PROJECTION = { BaseColumns._ID, Collection.COLLECTION_ID,
				Collection.COLLECTION_NAME, Collection.YEAR_PUBLISHED,
				Games.GAME_NAME, Games.GAME_ID, Games.THUMBNAIL_URL, };

		// int _ID = 0;
		// int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		int YEAR_PUBLISHED = 3;
		// int GAME_NAME = 4;
		int GAME_ID = 5;
		int THUMBNAIL_URL = 6;
	}
}
