package com.boardgamegeek.ui;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class ColorsActivity extends ListActivity implements AsyncQueryListener {
	private final String TAG = "ColorActivity";

	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final int MENU_COLOR_DELETE = Menu.FIRST;
	private static final int HELP_VERSION = 1;

	private ColorAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mGameColorUri;
	private String mGameName;
	private String mThumbnailUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_colors);
		getListView().setOnCreateContextMenuListener(this);

		processIntent();

		UIUtils.setGameHeader(this, mGameName, mThumbnailUrl);

		mAdapter = new ColorAdapter(this);
		setListAdapter(mAdapter);

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mGameColorUri, Query.PROJECTION);

		UIUtils.showHelpDialog(this, BggApplication.HELP_COLORS_KEY, HELP_VERSION, R.string.help_colors);
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
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
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
		final String color = cursor.getString(Query.COLOR);

		menu.setHeaderTitle(color);
		menu.add(0, MENU_COLOR_DELETE, 0, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuInfo", e);
			return false;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return false;
		}
		final String color = cursor.getString(Query.COLOR);

		switch (item.getItemId()) {
			case MENU_COLOR_DELETE: {
				getContentResolver().delete(Games.buildColorsUri(Games.getGameId(mGameColorUri), color), null, null);
				mHandler.startQuery(mGameColorUri, Query.PROJECTION);
				return true;
			}
		}
		return false;
	}

	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			UIUtils.showListMessage(this, R.string.empty_colors);
			mAdapter.changeCursor(cursor);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
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
