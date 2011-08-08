package com.boardgamegeek.ui;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class ColorsActivity extends ListActivity implements AsyncQueryListener {
	// private final String TAG = "ColorActivity";

	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";

	private ColorAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mGameColorUri;
	private String mGameName;
	private String mThumbnailUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_colors);

		processIntent();

		UIUtils.setGameHeader(this, mGameName, mThumbnailUrl);

		mAdapter = new ColorAdapter(this);
		setListAdapter(mAdapter);

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mGameColorUri, Query.PROJECTION);
	}

	private void processIntent() {
		final Intent intent = getIntent();
		mGameColorUri = intent.getData();
		if (intent.hasExtra(KEY_GAME_NAME)) {
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
		}
		if (intent.hasExtra(KEY_THUMBNAIL_URL)) {
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
		}
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		UIUtils.showListMessage(this, R.string.empty_colors);
		startManagingCursor(cursor);
		mAdapter.changeCursor(cursor);
	}

	private class ColorAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public ColorAdapter(Context context) {
			super(context, null);
			mInflater = getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.color.setText(cursor.getString(Query.COLOR));
		}
	}

	static class ViewHolder {
		TextView color;

		public ViewHolder(View view) {
			color = (TextView) view.findViewById(android.R.id.text1);
		}
	}

	private interface Query {
		String[] PROJECTION = { BaseColumns._ID, GameColors.COLOR, };
		// int _ID = 0;
		int COLOR = 1;
	}
}
