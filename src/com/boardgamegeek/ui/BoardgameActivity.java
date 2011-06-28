package com.boardgamegeek.ui;

import java.net.URLEncoder;

import org.apache.http.client.HttpClient;

import android.app.AlertDialog.Builder;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class BoardgameActivity extends TabActivity implements AsyncQueryListener {
	private static final String TAG = "BoardgameActivity";

	public static final String KEY_GAME_NAME = "GAME_NAME";
	private static final int HELP_VERSION = 1;
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final long REFRESH_THROTTLE_IN_HOURS = 1;

	private NotifyingAsyncQueryHandler mHandler;
	private Uri mGameUri;
	private boolean mShouldRetry;
	private boolean mIsLoaded;
	private boolean mIsRefreshing;
	private GameObserver mObserver;

	private int mId;
	private String mName;
	private String mThumbnailUrl;
	private String mImageUrl;
	private long mUpdatedDate;

	private View mUpdatePanel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_boardgame);
		UIUtils.setTitle(this);
		UIUtils.allowTypeToSearch(this);

		extractIntentInfo();
		setUiVariables();
		setupTabs();

		mShouldRetry = true;
		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		startQuery();

		showHelpDialog();
	}

	private void extractIntentInfo() {
		final Intent intent = getIntent();
		mGameUri = intent.getData();
		if (intent.hasExtra(KEY_GAME_NAME)) {
			mName = intent.getExtras().getString(KEY_GAME_NAME);
			UIUtils.setGameName(this, mName);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mGameUri, false, mObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		getContentResolver().unregisterContentObserver(mObserver);
	}

	private void setUiVariables() {
		mObserver = new GameObserver(null);
		mUpdatePanel = findViewById(R.id.update_panel);
	}

	private void startQuery() {
		mHandler.startQuery(mGameUri, GameQuery.PROJECTION);
		showLoadingMessage();
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

			mId = cursor.getInt(GameQuery.GAME_ID);
			mName = cursor.getString(GameQuery.GAME_NAME);
			mThumbnailUrl = cursor.getString(GameQuery.THUMBNAIL_URL);
			mImageUrl = cursor.getString(GameQuery.IMAGE_URL);
			mUpdatedDate = cursor.getLong(GameQuery.UPDATED);

			long lastUpdated = cursor.getLong(GameQuery.UPDATED);
			if (lastUpdated == 0 || DateTimeUtils.howManyDaysOld(lastUpdated) > AGE_IN_DAYS_TO_REFRESH) {
				refresh();
			}

			UIUtils u = new UIUtils(this);
			u.setGameName(mName);
			u.setThumbnail(mThumbnailUrl);
			mIsLoaded = true;

			if (!mIsRefreshing) {
				hideLoadingMessage();
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
		final Intent intent = new Intent(this, ImageActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra(ImageActivity.KEY_IMAGE_URL, mImageUrl);
		intent.putExtra(ImageActivity.KEY_GAME_NAME, mName);
		startActivity(intent);
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

	private void showHelpDialog() {
		if (BggApplication.getInstance().getShowBoardGameHelp(HELP_VERSION)) {
			Builder builder = new Builder(this);
			builder.setTitle(R.string.help_title)
					.setCancelable(false)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setMessage(R.string.help_boardgame)
					.setPositiveButton(R.string.help_button_close, null)
					.setNegativeButton(R.string.help_button_hide, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							BggApplication.getInstance().updateBoardGameHelp(HELP_VERSION);
						}
					});
			builder.create().show();
		}
	}

	private void showLoadingMessage() {
		mUpdatePanel.setVisibility(View.VISIBLE);
	}

	private void hideLoadingMessage() {
		mUpdatePanel.setVisibility(View.GONE);
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
				if (DateTimeUtils.howManyHoursOld(mUpdatedDate) > REFRESH_THROTTLE_IN_HOURS) {
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
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					startQuery();
				}
			});
		}
	}

	private class RefreshTask extends AsyncTask<String, Void, Boolean> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;

		@Override
		protected void onPreExecute() {
			mIsRefreshing = true;
			showLoadingMessage();
			mHttpClient = HttpUtils.createHttpClient(BoardgameActivity.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Boolean doInBackground(String... params) {
			String gameId = params[0];
			Log.d(TAG, "Refreshing game ID = " + gameId);
			final String url = HttpUtils.constructGameUrl(gameId);
			try {
				mExecutor.executeGet(url, new RemoteGameHandler());
			} catch (HandlerException e) {
				Log.e(TAG, "Exception trying to refresh game ID = " + gameId, e);
				showToastOnUiThread(R.string.msg_update_error);
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mIsRefreshing = false;
			hideLoadingMessage();
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
