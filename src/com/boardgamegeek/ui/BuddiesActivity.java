package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class BuddiesActivity extends ListActivity implements AsyncQueryListener, AbsListView.OnScrollListener {
	private static final String TAG = makeLogTag(BuddiesActivity.class);

	private Uri mUri;
	private BuddiesAdapter mAdapter;
	private NotifyingAsyncQueryHandler mHandler;
	private ThumbnailTask mThumbnailTask;
	private final BlockingQueue<Uri> mThumbnailQueue = new ArrayBlockingQueue<Uri>(12);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_buddies);

		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);
		getListView().setOnScrollListener(this);

		if (DateTimeUtils.howManyHoursOld(BggApplication.getInstance().getLastBuddiesSync()) > 2) {
			BggApplication.getInstance().putLastBuddiesSync();
			startService(new Intent(Intent.ACTION_SYNC, null, this, SyncService.class).putExtra(
				SyncService.KEY_SYNC_TYPE, SyncService.SYNC_TYPE_BUDDIES));
		}

		mAdapter = new BuddiesAdapter(this);
		setListAdapter(mAdapter);
		mUri = getIntent().getData();
	}

	private void startQuery() {
		if (mHandler == null) {
			mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		}
		mHandler.startQuery(mUri, BuddiesQuery.PROJECTION, Buddies.BUDDY_ID + "!=?", new String[] { "0" }, null);
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mUri, true, mBuddyObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		startQuery();
		mThumbnailTask = new ThumbnailTask();
		mThumbnailTask.execute();
	}

	@Override
	protected void onPause() {
		mThumbnailQueue.clear();
		mThumbnailTask.cancel(true);
		super.onPause();
	}

	@Override
	protected void onStop() {
		getContentResolver().unregisterContentObserver(mBuddyObserver);
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

	private void changeEmptyMessage() {
		TextView tv = (TextView) findViewById(R.id.list_message);
		tv.setText(R.string.empty_buddies);

		ProgressBar pb = (ProgressBar) findViewById(R.id.list_progress);
		pb.setVisibility(View.GONE);
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		final int buddyId = cursor.getInt(BuddiesQuery.BUDDY_ID);
		final Uri buddyUri = Buddies.buildBuddyUri(buddyId);
		startActivity(new Intent(Intent.ACTION_VIEW, buddyUri));
	}

	private ContentObserver mBuddyObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			startQuery();
		}
	};

	private class BuddiesAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public BuddiesAdapter(Context context) {
			super(context, null);
			mInflater = getLayoutInflater();
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row_buddy, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			int buddyId = cursor.getInt(BuddiesQuery.BUDDY_ID);
			String firstName = cursor.getString(BuddiesQuery.FIRSTNAME);
			String lastName = cursor.getString(BuddiesQuery.LASTNAME);
			holder.fullname.setText(buildFullName(firstName, lastName));
			holder.name.setText(cursor.getString(BuddiesQuery.NAME));
			holder.avatarUrl = Buddies.buildAvatarUri(buddyId);

			Drawable thumbnail = ImageUtils.getDrawable(BuddiesActivity.this, holder.avatarUrl);
			if (thumbnail == null) {
				holder.avatar.setVisibility(View.GONE);
			} else {
				holder.avatarUrl = null;
				holder.avatar.setImageDrawable(thumbnail);
				holder.avatar.setVisibility(View.VISIBLE);
			}
		}

		protected String buildFullName(String firstName, String lastName) {
			if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
				return "?";
			} else if (TextUtils.isEmpty(firstName)) {
				return lastName;
			} else if (TextUtils.isEmpty(firstName)) {
				return lastName;
			} else {
				return firstName + " " + lastName;
			}
		}
	}

	static class ViewHolder {
		TextView fullname;
		TextView name;
		BezelImageView avatar;
		Uri avatarUrl;

		public ViewHolder(View view) {
			fullname = (TextView) view.findViewById(R.id.list_fullname);
			name = (TextView) view.findViewById(R.id.list_name);
			avatar = (BezelImageView) view.findViewById(R.id.list_avatar);
		}
	}

	private class ThumbnailTask extends AsyncTask<Void, Void, Void> {

		private ListView mView;

		@Override
		protected void onPreExecute() {
			mView = BuddiesActivity.this.getListView();
		}

		@Override
		protected Void doInBackground(Void... params) {
			while (!isCancelled()) {
				try {
					Uri uri = mThumbnailQueue.take();
					ImageUtils.getAvatar(BuddiesActivity.this, uri);
					publishProgress();
				} catch (InterruptedException e) {
					LOGE(TAG, "getting buddies", e);
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			mView.invalidateViews();
		}
	}

	@Override
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
			if (vh.avatarUrl != null) {
				mThumbnailQueue.offer(vh.avatarUrl);
			}
		}
	}

	private interface BuddiesQuery {
		String[] PROJECTION = { BaseColumns._ID, Buddies.BUDDY_ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME,
			Buddies.BUDDY_LASTNAME };

		// int _ID = 0;
		int BUDDY_ID = 1;
		int NAME = 2;
		int FIRSTNAME = 3;
		int LASTNAME = 4;
	}
}
