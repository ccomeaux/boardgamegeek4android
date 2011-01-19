package com.boardgamegeek.ui;

import org.apache.http.client.HttpClient;

import android.app.TabActivity;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteGameHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class BoardgameActivity extends TabActivity implements AsyncQueryListener {
	private final static String TAG = "BoardgameActivity";

	private static final long THROTTLE_IN_MILLIS = 1800000; // 1 hour

	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;
	private boolean mRetry;

	private int mId;
	private String mName;
	private String mThumbnailUrl;
	private String mImageUrl;
	private long mUpdatedDate;

	private TextView mNameView;
	private ImageView mThumbnail;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_boardgame);
		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		final Intent intent = getIntent();
		mBoardgameUri = intent.getData();

		setUiVariables();
		setupInfoTab();
		setupStatsTab();

		getContentResolver().registerContentObserver(mBoardgameUri, true, new GameObserver(null));

		mRetry = true;
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mBoardgameUri, BoardgameQuery.PROJECTION);
	}

	private void setUiVariables() {
		mNameView = (TextView) findViewById(R.id.game_name);
		mThumbnail = (ImageView) findViewById(R.id.game_thumbnail);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (!cursor.moveToFirst()) {
				if (mRetry) {
					mRetry = false;
					Refresh();
				}
				return;
			}

			mId = cursor.getInt(BoardgameQuery.GAME_ID);
			mName = cursor.getString(BoardgameQuery.GAME_NAME);
			mThumbnailUrl = cursor.getString(BoardgameQuery.THUMBNAIL_URL);
			mImageUrl = cursor.getString(BoardgameQuery.IMAGE_URL);
			mUpdatedDate = cursor.getLong(BoardgameQuery.UPDATED_DETAIL);

			mNameView.setText(mName);

			if (BggApplication.getInstance().getImageLoad() && !TextUtils.isEmpty(mThumbnailUrl)) {
				new ThumbnailTask().execute(mThumbnailUrl);
			}

		} finally {
			cursor.close();
		}
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

	public void onThumbnailClick(View v) {
		Intent i = new Intent(this, ImageActivity.class);
		i.setAction(Intent.ACTION_VIEW);
		i.putExtra(ImageActivity.KEY_IMAGE_URL, mImageUrl);
		i.putExtra(ImageActivity.KEY_GAME_NAME, mName);
		startActivity(i);
	}

	private void setupInfoTab() {
		final TabHost host = getTabHost();

		final Intent intent = new Intent(this, GameInfoActivityTab.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(mBoardgameUri);
		intent.addCategory(Intent.CATEGORY_TAB);

		host.addTab(host.newTabSpec("info").setIndicator(buildIndicator(R.string.tab_title_info)).setContent(intent));
	}

	private void setupStatsTab() {
		final TabHost host = getTabHost();

		final Intent intent = new Intent(this, GameStatsActivityTab.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(mBoardgameUri);
		intent.addCategory(Intent.CATEGORY_TAB);

		host.addTab(host.newTabSpec("stats").setIndicator(buildIndicator(R.string.tab_title_stats)).setContent(intent));
	}

	private View buildIndicator(int textRes) {
		final TextView indicator = (TextView) getLayoutInflater()
				.inflate(R.layout.tab_indicator, getTabWidget(), false);
		indicator.setText(textRes);
		return indicator;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.boardgame, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// only allow logging a play once the game is populated
		MenuItem mi = menu.findItem(R.id.log_play);
		mi.setEnabled(!TextUtils.isEmpty(mName));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.log_play:
				logPlay(false);
				return true;
			case R.id.log_play_quick:
				logPlay(true);
				return true;
			case R.id.refresh:
				long now = System.currentTimeMillis();
				if (now - mUpdatedDate > THROTTLE_IN_MILLIS) {
					Refresh();
				} else {
					showToast(R.string.msg_refresh_exceeds_throttle);
				}
				return true;
		}
		return false;
	}

	private void logPlay(boolean quick) {
		Intent intent = new Intent(this, LogPlayActivity.class);
		intent.setAction(quick ? Intent.ACTION_VIEW : Intent.ACTION_EDIT);
		intent.putExtra(LogPlayActivity.KEY_GAME_ID, mId);
		intent.putExtra(LogPlayActivity.KEY_GAME_NAME, mName);
		intent.putExtra(LogPlayActivity.KEY_THUMBNAIL_URL, mThumbnailUrl);
		startActivity(intent);
	}

	private void Refresh() {
		new RefreshTask().execute(mBoardgameUri.getLastPathSegment());
	}

	private void showToast(int messageId) {
		Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
	}

	class GameObserver extends ContentObserver {

		public GameObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG, "Caught changed URI = " + mBoardgameUri);
			mHandler.startQuery(mBoardgameUri, BoardgameQuery.PROJECTION);
			runOnUiThread(new Runnable() {
				public void run() {
					showToast(R.string.msg_updated);
				}
			});
		}
	}

	private class RefreshTask extends AsyncTask<String, Void, Void> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(BoardgameActivity.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Void doInBackground(String... params) {
			String gameId = params[0];
			Log.d(TAG, "Refreshing game ID = " + gameId);
			final String url = HttpUtils.constructGameUrl(gameId);
			try {
				mExecutor.executeGet(url, new RemoteGameHandler());
			} catch (HandlerException e) {
				Log.e(TAG, "Exception trying to refresh game ID = " + gameId, e);
				runOnUiThread(new Runnable() {
					public void run() {
						showToast(R.string.msg_error_remote);
					}
				});
			}
			return null;
		}
	}

	private class ThumbnailTask extends AsyncTask<String, Void, Bitmap> {

		@Override
		protected void onPreExecute() {
			findViewById(R.id.thumbnail_progress).setVisibility(View.VISIBLE);
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			return ImageCache.getImage(BoardgameActivity.this, params[0]);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			findViewById(R.id.thumbnail_progress).setVisibility(View.GONE);
			mThumbnail.setVisibility(View.VISIBLE);
			if (result != null) {
				mThumbnail.setImageBitmap(result);
			} else {
				mThumbnail.setImageResource(R.drawable.noimage);
			}
		}
	}

	private interface BoardgameQuery {
		String[] PROJECTION = { Games._ID, Games.GAME_NAME, Games.GAME_ID, Games.THUMBNAIL_URL, Games.IMAGE_URL,
				Games.UPDATED_DETAIL, };

		// int ID = 0;
		int GAME_NAME = 1;
		int GAME_ID = 2;
		int THUMBNAIL_URL = 3;
		int IMAGE_URL = 4;
		int UPDATED_DETAIL = 5;
	}
}
