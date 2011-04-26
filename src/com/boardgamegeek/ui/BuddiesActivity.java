package com.boardgamegeek.ui;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

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

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		changeEmptyMessage();
		startManagingCursor(cursor);
		mAdapter.changeCursor(cursor);
	}
	
	private void changeEmptyMessage() {
		TextView tv = (TextView) findViewById(R.id.listMessage);
		tv.setText(R.string.empty_buddies);

		ProgressBar pb = (ProgressBar) findViewById(R.id.listProgress);
		pb.setVisibility(View.GONE);
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor)mAdapter.getItem(position);
		final int buddyId = cursor.getInt(BuddiesQuery.BUDDY_ID);
		final Uri buddyUri = Buddies.buildBuddyUri(buddyId);
		startActivity(new Intent(Intent.ACTION_VIEW, buddyUri));
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
			textView.setText(cursor.getString(BuddiesQuery.FIRSTNAME) + " " + cursor.getString(BuddiesQuery.LASTNAME));

			final TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
			textView2.setText(cursor.getString(BuddiesQuery.NAME));
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = {
			BaseColumns._ID,
			Buddies.BUDDY_ID,
			Buddies.BUDDY_NAME,
			Buddies.BUDDY_FIRSTNAME,
			Buddies.BUDDY_LASTNAME,
		};

		//int _ID = 0;
		int BUDDY_ID = 1;
		int NAME = 2;
		int FIRSTNAME = 3;
		int LASTNAME = 4;
	}
}
