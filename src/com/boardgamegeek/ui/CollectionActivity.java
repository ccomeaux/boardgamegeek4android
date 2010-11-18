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
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;

public class CollectionActivity extends ListActivity  implements AsyncQueryListener {

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
		mHandler.startQuery(uri, CollectionQuery.PROJECTION, null, null, Games.DEFAULT_SORT);
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

	// protected void onListItemClick(ListView l, View v, int position, long id)
	// {
	// final Cursor cursor = (Cursor)mAdapter.getItem(position);
	// final int gameId = cursor.getInt(CollectionQuery.GAME_ID);
	// final Uri gameUri = Games.buildGameUri(gameId);
	// startActivity(new Intent(Intent.ACTION_VIEW, gameUri));
	// }
	
	private class CollectionAdapter extends CursorAdapter {
		public CollectionAdapter(Context context) {
			super(context, null);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final TextView textView = (TextView) view.findViewById(android.R.id.text1);
			textView.setText(cursor.getString(CollectionQuery.GAME_NAME));

			final TextView textView2 = (TextView) view.findViewById(android.R.id.text2);
			textView2.setText(cursor.getString(CollectionQuery.YEAR_PUBLISHED));
		}
	}
	
	private interface CollectionQuery {
		String[] PROJECTION = {
			BaseColumns._ID,
			Games.GAME_ID,
			Games.GAME_NAME,
			Games.YEAR_PUBLISHED,
		};

		//int _ID = 0;
		int GAME_ID = 1;
		int GAME_NAME = 2;
		int YEAR_PUBLISHED = 3;
	}
}
