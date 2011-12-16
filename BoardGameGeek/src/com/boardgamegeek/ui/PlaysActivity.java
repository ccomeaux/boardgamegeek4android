package com.boardgamegeek.ui;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class PlaysActivity extends ListActivity implements AsyncQueryListener {
	// private static final String TAG = "PlaysActivity";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";

	private PlaysAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plays);

		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		mAdapter = new PlaysAdapter(this);
		setListAdapter(mAdapter);

		mUri = getIntent().getData();

		if (mUri.getPathSegments().contains("games")) {
			Bundle extras = getIntent().getExtras();
			UIUtils.setGameHeader(this, extras.getString(KEY_GAME_NAME), extras.getString(KEY_THUMBNAIL_URL));
		} else {
			findViewById(R.id.game_header).setVisibility(View.GONE);
			findViewById(R.id.header_divider).setVisibility(View.GONE);
		}
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		startQuery();
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mUri, true, mPlaysObserver);
	}

	@Override
	protected void onStop() {
		getContentResolver().unregisterContentObserver(mPlaysObserver);
		super.onStop();
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

	protected void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		int playId = cursor.getInt(Query.PLAY_ID);
		Uri buddyUri = Plays.buildPlayUri(playId);
		Intent i = new Intent(Intent.ACTION_VIEW, buddyUri);
		i.putExtra(PlayActivity.KEY_GAME_ID, cursor.getInt(Query.GAME_ID));
		i.putExtra(PlayActivity.KEY_GAME_NAME, cursor.getString(Query.GAME_NAME));
		startActivity(i);
	}

	private void startQuery() {
		mHandler.startQuery(mUri, Query.PROJECTION, null, null, Plays.DEFAULT_SORT);
	}

	private void changeEmptyMessage() {
		TextView tv = (TextView) findViewById(R.id.list_message);
		tv.setText(R.string.empty_plays);

		ProgressBar pb = (ProgressBar) findViewById(R.id.list_progress);
		pb.setVisibility(View.GONE);
	}

	private ContentObserver mPlaysObserver = new ContentObserver(new Handler()) {
		private static final long OBSERVER_THROTTLE_IN_MILLIS = 10000; // 10s

		private long mLastUpdated;

		@Override
		public void onChange(boolean selfChange) {
			long now = System.currentTimeMillis();
			if (now - mLastUpdated > OBSERVER_THROTTLE_IN_MILLIS) {
				startQuery();
				mLastUpdated = System.currentTimeMillis();
			}
		}
	};

	private class PlaysAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public PlaysAdapter(Context context) {
			super(context, null);
			mInflater = getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_play, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.date.setText(cursor.getString(Query.DATE));
			holder.name.setText(cursor.getString(Query.GAME_NAME));
			holder.location.setText(cursor.getString(Query.LOCATION));
		}
	}

	static class ViewHolder {
		TextView name;
		TextView date;
		TextView location;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.list_name);
			date = (TextView) view.findViewById(R.id.list_date);
			location = (TextView) view.findViewById(R.id.list_location);
		}
	}

	private interface Query {
		String[] PROJECTION = { BaseColumns._ID, Plays.PLAY_ID, Plays.DATE, PlayItems.NAME, PlayItems.OBJECT_ID,
				Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH };

		// int _ID = 0;
		int PLAY_ID = 1;
		int DATE = 2;
		int GAME_NAME = 3;
		int GAME_ID = 4;
		int LOCATION = 5;
		// int QUANTITY = 6;
		// int LENGTH = 7;
	}
}
