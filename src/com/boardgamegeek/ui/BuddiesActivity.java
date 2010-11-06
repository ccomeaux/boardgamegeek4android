package com.boardgamegeek.ui;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class BuddiesActivity extends ListActivity implements AsyncQueryListener {

	private BuddiesAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_buddies);

		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		mAdapter = new BuddiesAdapter(this);
		setListAdapter(mAdapter);

		Uri uri = getIntent().getData();
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(uri, BuddiesQuery.PROJECTION, null, null, Buddies.DEFAULT_SORT);
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

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		startManagingCursor(cursor);
		mAdapter.changeCursor(cursor);
	}

	private class BuddiesAdapter extends CursorAdapter {
		public BuddiesAdapter(Context context) {
			super(context, null);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final TextView textView = (TextView) view.findViewById(android.R.id.text1);
			textView.setText(cursor.getString(BuddiesQuery.BUDDY_NAME));

			final TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
			textView2.setText(cursor.getString(BuddiesQuery.BUDDY_ID));
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = { BaseColumns._ID, SyncColumns.UPDATED, Buddies.BUDDY_ID, Buddies.BUDDY_NAME, };

		//int _ID = 0;
		//int UPDATED = 1;
		int BUDDY_ID = 2;
		int BUDDY_NAME = 3;
	}
}
