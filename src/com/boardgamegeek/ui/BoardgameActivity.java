package com.boardgamegeek.ui;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.UIUtils;

public class BoardgameActivity extends TabActivity implements
		AsyncQueryListener {
	private final static String TAG = "BoardgameActivity";

	private static HttpClient sHttpClient;

	private Uri mBoardgameUri;
	private NotifyingAsyncQueryHandler mHandler;

	private int mId;
	private String mName;

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
				return;
			}

			mId = cursor.getInt(BoardgameQuery.GAME_ID);
			mName = cursor.getString(BoardgameQuery.GAME_NAME);

			mNameView.setText(mName);

			final String url = cursor.getString(BoardgameQuery.THUMBNAIL_URL);
			new ImageTask().execute(url);

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

	private void setupInfoTab() {
		final TabHost host = getTabHost();

		final Intent intent = new Intent(this, GameInfoActivityTab.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(mBoardgameUri);
		intent.addCategory(Intent.CATEGORY_TAB);

		host.addTab(host.newTabSpec("info")
				.setIndicator(buildIndicator(R.string.tab_title_info))
				.setContent(intent));
	}

	private void setupStatsTab() {
		final TabHost host = getTabHost();

		final Intent intent = new Intent(this, GameStatsActivityTab.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(mBoardgameUri);
		intent.addCategory(Intent.CATEGORY_TAB);

		host.addTab(host.newTabSpec("stats")
				.setIndicator(buildIndicator(R.string.tab_title_stats))
				.setContent(intent));
	}

	private View buildIndicator(int textRes) {
		final TextView indicator = (TextView) getLayoutInflater().inflate(
				R.layout.tab_indicator, getTabWidget(), false);
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
		}
		return false;
	}

	private void logPlay(boolean quick) {
		Intent intent = new Intent(this, LogPlayActivity.class);
		intent.setAction(quick ? Intent.ACTION_VIEW : Intent.ACTION_EDIT);
		intent.putExtra("GAME_ID", mId);
		intent.putExtra("GAME_NAME", mName);
		startActivity(intent);
	}

	private static synchronized HttpClient getHttpClient(Context context) {
		if (sHttpClient == null) {
			sHttpClient = HttpUtils.createHttpClient(context, true);
		}
		return sHttpClient;
	}

	private class ImageTask extends AsyncTask<String, Void, Bitmap> {

		@Override
		protected void onPreExecute() {
			if (BggApplication.getInstance().getImageLoad()) {
				findViewById(R.id.thumbnail_progress).setVisibility(
						View.VISIBLE);
			}
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			if (!BggApplication.getInstance().getImageLoad()) {
				return null;
			}

			final String url = params[0];

			try {
				final Context context = BoardgameActivity.this;
				final HttpClient client = getHttpClient(context);
				final HttpGet get = new HttpGet(url);
				final HttpResponse response = client.execute(get);
				final HttpEntity entity = response.getEntity();

				final int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK || entity == null) {
					Log.w(TAG, "Didn't find avatar");
				}

				final byte[] imageData = EntityUtils.toByteArray(entity);
				return BitmapFactory.decodeByteArray(imageData, 0,
						imageData.length);

			} catch (Exception e) {
				Log.e(TAG, "Problem loading thumbnail", e);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (BggApplication.getInstance().getImageLoad()) {
				findViewById(R.id.thumbnail_progress).setVisibility(View.GONE);
				mThumbnail.setVisibility(View.VISIBLE);
				if (result != null) {
					mThumbnail.setImageBitmap(result);
				}
			}
		}
	}

	private interface BoardgameQuery {
		String[] PROJECTION = { Games._ID, Games.GAME_NAME, Games.GAME_ID,
				Games.THUMBNAIL_URL, };

		// int ID = 0;
		int GAME_NAME = 1;
		int GAME_ID = 2;
		int THUMBNAIL_URL = 3;
	}
}
