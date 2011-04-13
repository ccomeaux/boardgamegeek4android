package com.boardgamegeek.ui;

import java.net.URLEncoder;

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
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class BoardgameActivity extends TabActivity implements AsyncQueryListener {
	private final static String TAG = "BoardgameActivity";

	private static final long REFRESH_THROTTLE_IN_MILLIS = 3600000; // 1 hour

	private NotifyingAsyncQueryHandler mHandler;
	private Uri mGameUri;
	private boolean mShouldRetry;
	private boolean mIsLoaded;
	private GameObserver mObserver;

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

		mGameUri = getIntent().getData();

		setUiVariables();
		setupTabs();

		mShouldRetry = true;
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(mGameUri, GameQuery.PROJECTION);
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mGameUri, false, mObserver);
	}

	@Override
	protected void onStop() {
		super.onStop();
		getContentResolver().unregisterContentObserver(mObserver);
	}

	private void setUiVariables() {
		mObserver = new GameObserver(null);
		mNameView = (TextView) findViewById(R.id.game_name);
		mThumbnail = (ImageView) findViewById(R.id.game_thumbnail);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (!cursor.moveToFirst()) {
				if (mShouldRetry) {
					mShouldRetry = false;
					refresh();
				}
				return;
			}

			findViewById(R.id.loading).setVisibility(View.GONE);
			findViewById(android.R.id.tabhost).setVisibility(View.VISIBLE);

			mId = cursor.getInt(GameQuery.GAME_ID);
			mName = cursor.getString(GameQuery.GAME_NAME);
			mThumbnailUrl = cursor.getString(GameQuery.THUMBNAIL_URL);
			mImageUrl = cursor.getString(GameQuery.IMAGE_URL);
			mUpdatedDate = cursor.getLong(GameQuery.UPDATED);

			mNameView.setText(mName);
			mIsLoaded = true;

			long lastUpdated = cursor.getLong(GameQuery.UPDATED);
			if (lastUpdated == 0 || DateTimeUtils.howManyDaysOld(lastUpdated) > 7) {
				refresh();
			}

			if (BggApplication.getInstance().getImageLoad() && !TextUtils.isEmpty(mThumbnailUrl)
					&& mThumbnail.getVisibility() != View.VISIBLE) {
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

	private void setupTabs() {
		setupTab(GameInfoActivityTab.class, "info", R.string.tab_title_info);
		setupTab(GameStatsActivityTab.class, "stats", R.string.tab_title_stats);
		setupTab(GameListsActivityTab.class, "lists", R.string.tab_title_lists);
		setupTab(GamePollsActivityTab.class, "polls", R.string.tab_title_polls);
	}

	private void setupTab(Class<?> cls, String tag, int indicatorResource) {
		final TabHost host = getTabHost();

		final Intent intent = new Intent(this, cls);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(mGameUri);
		intent.addCategory(Intent.CATEGORY_TAB);

		host.addTab(host.newTabSpec(tag).setIndicator(buildIndicator(indicatorResource)).setContent(intent));
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
		// only enable options when the game is loaded

		enableMenuItem(menu, R.id.log_play);
		enableMenuItem(menu, R.id.log_play_quick);
		enableMenuItem(menu, R.id.refresh);
		enableMenuItem(menu, R.id.links);
		enableMenuItem(menu, R.id.share);

		return super.onPrepareOptionsMenu(menu);
	}

	private void enableMenuItem(Menu menu, int id) {
		MenuItem mi = menu.findItem(id);
		if (mi != null) {
			mi.setEnabled(mIsLoaded);
		}
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
				if (now - mUpdatedDate > REFRESH_THROTTLE_IN_MILLIS) {
					refresh();
				} else {
					showToast(R.string.msg_refresh_exceeds_throttle);
				}
				return true;
			case R.id.menu_bgg:
				link("http://www.boardgamegeek.com/boardgame/" + mId);
				return true;
			case R.id.menu_bg_prices:
				link("http://boardgameprices.com/iphone/?s=" + URLEncoder.encode(mName));
				return true;
			case R.id.menu_amazon:
				link("http://www.amazon.com/gp/aw/s.html/?m=aps&k=" + URLEncoder.encode(mName)
						+ "&i=toys-and-games&submitSearch=GO");
				return true;
			case R.id.menu_ebay:
				link("http://shop.mobileweb.ebay.com/searchresults?kw=" + URLEncoder.encode(mName));
				return true;
			case R.id.share:
				share();
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

	private void refresh() {
		new RefreshTask().execute(mGameUri.getLastPathSegment());
	}

	private void link(String link) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
	}

	private void share() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT,
				String.format(getResources().getString(R.string.share_subject), mName));
		shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(getResources().getString(R.string.share_text), mName,
				"http://www.boardgamegeek.com/boardgame/" + mId));
		startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_title)));
	}

	private void showToast(int messageId) {
		Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
	}

	private void showToastOnUiThread(final int messageId) {
		runOnUiThread(new Runnable() {
			public void run() {
				showToast(messageId);
			}
		});
	}

	class GameObserver extends ContentObserver {

		public GameObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(mGameUri, GameQuery.PROJECTION);
			showToastOnUiThread(R.string.msg_updated);
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
				showToastOnUiThread(R.string.msg_updated);
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

	private interface GameQuery {
		String[] PROJECTION = { Games._ID, Games.GAME_NAME, Games.GAME_ID, Games.THUMBNAIL_URL, Games.IMAGE_URL,
				Games.UPDATED, };

		// int ID = 0;
		int GAME_NAME = 1;
		int GAME_ID = 2;
		int THUMBNAIL_URL = 3;
		int IMAGE_URL = 4;
		int UPDATED = 5;
	}
}
