package com.boardgamegeek.ui;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.LogInHelper;
import com.boardgamegeek.util.LogInHelper.LogInListener;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class PlaysActivity extends ListActivity implements AsyncQueryListener, LogInListener {
	private static final String TAG = "PlaysActivity";

	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";

	private static final int MENU_PLAY_EDIT = Menu.FIRST;
	private static final int MENU_PLAY_DELETE = Menu.FIRST + 1;

	private LogInHelper mLogInHelper;
	private PlaysAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plays);

		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);
		getListView().setOnCreateContextMenuListener(this);
		mLogInHelper = new LogInHelper(this, this);

		if (DateTimeUtils.howManyHoursOld(BggApplication.getInstance().getLastPlaysSync()) > 2) {
			BggApplication.getInstance().putLastPlaysSync();
			startService(new Intent(Intent.ACTION_SYNC, null, this, SyncService.class).putExtra(
					SyncService.KEY_SYNC_TYPE, SyncService.SYNC_TYPE_PLAYS));
		}

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
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(Plays.CONTENT_URI, true, mPlaysObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		startQuery();
		mLogInHelper.logIn();
	}

	@Override
	protected void onStop() {
		getContentResolver().unregisterContentObserver(mPlaysObserver);
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
		mAdapter.changeCursor(cursor);
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		viewPlay(cursor);
	}

	private void viewPlay(Cursor cursor) {
		int playId = cursor.getInt(Query.PLAY_ID);
		Uri buddyUri = Plays.buildPlayUri(playId);
		Intent i = new Intent(Intent.ACTION_VIEW, buddyUri);
		i.putExtra(PlayActivity.KEY_GAME_ID, cursor.getInt(Query.GAME_ID));
		i.putExtra(PlayActivity.KEY_GAME_NAME, cursor.getString(Query.GAME_NAME));
		startActivity(i);
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
		final String gameName = cursor.getString(Query.GAME_NAME);

		menu.setHeaderTitle(gameName);
		menu.add(0, MENU_PLAY_EDIT, 0, R.string.menu_edit);
		MenuItem mi = menu.add(0, MENU_PLAY_DELETE, 0, R.string.menu_delete);
		mi.setEnabled(mLogInHelper.checkCookies());
		// TODO: add Send and Share menu items
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

		switch (item.getItemId()) {
			case MENU_PLAY_EDIT: {
				ActivityUtils.logPlay(this, cursor.getInt(Query.PLAY_ID), cursor.getInt(Query.GAME_ID),
						cursor.getString(Query.GAME_NAME), null);
				return true;
			}
			case MENU_PLAY_DELETE: {
				final int playId = cursor.getInt(Query.PLAY_ID);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.are_you_sure_title).setMessage(R.string.are_you_sure_delete_play)
						.setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								ActivityUtils.deletePlay(PlaysActivity.this, mLogInHelper.getCookieStore(), playId);
							}
						}).setNegativeButton(R.string.no, null);
				builder.create().show();
				return true;
			}
		}
		return false;
	}

	private void startQuery() {
		if (mHandler == null) {
			mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		}
		mHandler.startQuery(mUri, Query.PROJECTION, null, null, Plays.DEFAULT_SORT);
	}

	private void changeEmptyMessage() {
		TextView tv = (TextView) findViewById(R.id.list_message);
		tv.setText(R.string.empty_plays);

		ProgressBar pb = (ProgressBar) findViewById(R.id.list_progress);
		pb.setVisibility(View.GONE);
	}

	private ContentObserver mPlaysObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			startQuery();
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
			if (cursor.getInt(Query.SYNC_STATUS) != Play.SYNC_STATUS_SYNCED) {
				view.setBackgroundResource(R.color.background_light);
			} else {
				view.setBackgroundResource(R.color.background);
			}
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
				Plays.LOCATION, Plays.QUANTITY, Plays.LENGTH, Plays.SYNC_STATUS };

		int PLAY_ID = 1;
		int DATE = 2;
		int GAME_NAME = 3;
		int GAME_ID = 4;
		int LOCATION = 5;
		int SYNC_STATUS = 8;
	}

	@Override
	public void onLogInSuccess() {
		// do nothing
	}

	@Override
	public void onLogInError(String errorMessage) {
		Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onNeedCredentials() {
		Toast.makeText(this, R.string.setUsernamePassword, Toast.LENGTH_LONG).show();
	}
}
