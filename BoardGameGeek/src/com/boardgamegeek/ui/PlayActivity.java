package com.boardgamegeek.ui;

import org.apache.http.client.HttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.LogInHelper;
import com.boardgamegeek.util.LogInHelper.LogInListener;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class PlayActivity extends Activity implements AsyncQueryListener, LogInListener {
	private final static String TAG = "PlayActivity";

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";

	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYER = 2;
	private static final int TOKEN_GAME = 3;

	private static final int AGE_IN_DAYS_TO_REFRESH = 7;

	private LogInHelper mLogInHelper;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mPlayUri;
	private Uri mPlayerUri;
	private PlayObserver mObserver;

	private int mGameId;
	private String mGameName;
	private String mThumbnailUrl;
	private Play mPlay;

	private TextView mUpdated;
	private TextView mDate;
	private TextView mQuantity;
	private TextView mLength;
	private TextView mLocation;
	private CheckBox mIncomplete;
	private CheckBox mNoWinStats;
	private TextView mComments;
	private LinearLayout mPlayerList;
	private View mUpdatePanel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		setUiVariables();
		mLogInHelper = new LogInHelper(this, this);
		parseIntent();
		UIUtils.setGameHeader(this, mGameName, mThumbnailUrl);

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		startQuery();
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mPlayUri, true, mObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mLogInHelper.logIn();
	}

	@Override
	protected void onStop() {
		getContentResolver().unregisterContentObserver(mObserver);
		super.onStop();
	}

	private void parseIntent() {
		Intent intent = getIntent();
		mGameId = intent.getExtras().getInt(KEY_GAME_ID);
		mGameName = intent.getExtras().getString(KEY_GAME_NAME);
		mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);

		if (mGameId == -1) {
			Log.w(TAG, "Didn't get a game ID");
			finish();
		}

		mPlayUri = intent.getData();
		int playId = Plays.getPlayId(mPlayUri);
		mPlayerUri = Plays.buildPlayerUri(playId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.play, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem mi = menu.findItem(R.id.menu_delete);
		mi.setEnabled(mLogInHelper.checkCookies());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_edit:
				ActivityUtils.logPlay(this, mPlay.PlayId, mGameId, mGameName, mThumbnailUrl);
				return true;
			case R.id.menu_delete:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.are_you_sure_title).setMessage(R.string.are_you_sure_delete_play)
						.setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								boolean deleted = ActivityUtils.deletePlay(PlayActivity.this,
										mLogInHelper.getCookieStore(), mPlay.PlayId);
								if (deleted) {
									finish();
								}
							}
						}).setNegativeButton(R.string.no, null);
				builder.create().show();
				return true;
		}
		return false;
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
		try {
			if (token == TOKEN_PLAY) {
				if (!cursor.moveToFirst()) {
					return;
				}

				mGameName = cursor.getString(Query.NAME);
				UIUtils.setGameName(this, mGameName);

				mPlay = new Play();
				mPlay.populate(cursor);
				bindUi();

				long lastUpdated = cursor.getLong(Query.UPDATED_LIST);
				if (lastUpdated == 0 || DateTimeUtils.howManyDaysOld(lastUpdated) > AGE_IN_DAYS_TO_REFRESH) {
					refresh();
				}

			} else if (token == TOKEN_PLAYER) {
				while (cursor.moveToNext()) {
					Player player = new Player(cursor);
					mPlay.addPlayer(player);
				}
				bindUi();
			} else if (token == TOKEN_GAME) {
				if (!cursor.moveToFirst()) {
					return;
				}

				mThumbnailUrl = cursor.getString(GameQuery.THUMBNAIL_URL);
				new UIUtils(this).setThumbnail(mThumbnailUrl);
			}
		} finally {
			cursor.close();
		}
	}

	private void setUiVariables() {
		mUpdated = (TextView) findViewById(R.id.updated);
		mDate = (TextView) findViewById(R.id.play_date);
		mQuantity = (TextView) findViewById(R.id.play_quantity);
		mLength = (TextView) findViewById(R.id.play_length);
		mLocation = (TextView) findViewById(R.id.play_location);
		mIncomplete = (CheckBox) findViewById(R.id.play_incomplete);
		mNoWinStats = (CheckBox) findViewById(R.id.play_no_win_stats);
		mComments = (TextView) findViewById(R.id.play_comments);
		mPlayerList = (LinearLayout) findViewById(R.id.play_player_list);
		mUpdatePanel = findViewById(R.id.update_panel);
		mObserver = new PlayObserver(null);
	}

	private void bindUi() {
		mDate.setText(mPlay.getFormattedDate());

		mQuantity.setText(String.valueOf(mPlay.Quantity));
		mQuantity.setVisibility((mPlay.Quantity == 1) ? View.GONE : View.VISIBLE);
		findViewById(R.id.play_quantity_label).setVisibility((mPlay.Quantity == 1) ? View.GONE : View.VISIBLE);

		mLength.setText(String.valueOf(mPlay.Length));
		mLength.setVisibility((mPlay.Length == 0) ? View.GONE : View.VISIBLE);
		findViewById(R.id.play_length_label).setVisibility((mPlay.Length == 0) ? View.GONE : View.VISIBLE);

		mLocation.setText(mPlay.Location);
		mLocation.setVisibility(TextUtils.isEmpty(mPlay.Location) ? View.GONE : View.VISIBLE);
		findViewById(R.id.play_location_label).setVisibility(
				TextUtils.isEmpty(mPlay.Location) ? View.GONE : View.VISIBLE);

		mIncomplete.setVisibility(mPlay.Incomplete ? View.VISIBLE : View.GONE);
		mNoWinStats.setVisibility(mPlay.NoWinStats ? View.VISIBLE : View.GONE);

		mComments.setText(mPlay.Comments);
		mComments.setVisibility(TextUtils.isEmpty(mPlay.Comments) ? View.GONE : View.VISIBLE);
		findViewById(R.id.play_comments_label).setVisibility(
				TextUtils.isEmpty(mPlay.Comments) ? View.GONE : View.VISIBLE);

		findViewById(R.id.play_player_label).setVisibility((mPlay.getPlayers().size() == 0) ? View.GONE : View.VISIBLE);
		for (Player player : mPlay.getPlayers()) {
			PlayerRow pr = new PlayerRow(this);
			pr.hideButtons();
			pr.setPlayer(player);
			mPlayerList.addView(pr);
		}

		long updated = mPlay.Updated;
		if (updated == 0) {
			mUpdated.setVisibility(View.GONE);
		} else {
			mUpdated.setVisibility(View.VISIBLE);
			CharSequence u = DateUtils.getRelativeTimeSpanString(updated, System.currentTimeMillis(),
					DateUtils.MINUTE_IN_MILLIS);
			mUpdated.setText(getResources().getString(R.string.updated) + " " + u);
		}
	}

	private interface Query {
		String[] PROJECTION = { Plays.PLAY_ID, PlayItems.NAME, PlayItems.OBJECT_ID, Plays.DATE, Plays.LOCATION,
				Plays.LENGTH, Plays.QUANTITY, Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.COMMENTS, Plays.UPDATED_LIST };

		int NAME = 1;
		int UPDATED_LIST = 10;
	}

	private interface PlayerQuery {
		String[] PROJECTION = { PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.START_POSITION, PlayPlayers.COLOR,
				PlayPlayers.SCORE, PlayPlayers.RATING, PlayPlayers.NEW, PlayPlayers.WIN, };
	}

	private interface GameQuery {
		String[] PROJECTION = { Games.THUMBNAIL_URL };

		int THUMBNAIL_URL = 0;
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

	class PlayObserver extends ContentObserver {

		public PlayObserver(Handler handler) {
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

	private void startQuery() {
		mHandler.startQuery(TOKEN_PLAY, mPlayUri, Query.PROJECTION);
		mHandler.startQuery(TOKEN_PLAYER, mPlayerUri, PlayerQuery.PROJECTION);
		if (TextUtils.isEmpty(mThumbnailUrl)) {
			Uri uri = Games.buildGameUri(mGameId);
			mHandler.startQuery(TOKEN_GAME, uri, GameQuery.PROJECTION);
		}
	}

	private void refresh() {
		new RefreshTask().execute(String.valueOf(mGameId), mPlay.getFormattedDate());
	}

	private class RefreshTask extends AsyncTask<String, Void, Boolean> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private long mStartTime;

		@Override
		protected void onPreExecute() {
			mStartTime = System.currentTimeMillis();
			showLoadingMessage();
			mHttpClient = HttpUtils.createHttpClient(PlayActivity.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Boolean doInBackground(String... params) {
			int gameId = StringUtils.parseInt(params[0]);
			String date = params[1];
			Log.d(TAG, "Refreshing game ID [" + gameId + "] on [" + date + "]");
			final String url = HttpUtils.constructPlayUrlSpecific(gameId, null);
			try {
				RemotePlaysHandler handler = new RemotePlaysHandler();
				mExecutor.executeGet(url, handler);
			} catch (HandlerException e) {
				Log.e(TAG, "Exception trying to refresh game ID [" + gameId + "] on [" + date + "]", e);
				showToastOnUiThread(R.string.msg_update_error);
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			hideLoadingMessage();
			Log.d(TAG, "Refresh took " + (System.currentTimeMillis() - mStartTime) + "ms");
		}
	}

	private void showLoadingMessage() {
		mUpdatePanel.setVisibility(View.VISIBLE);
	}

	private void hideLoadingMessage() {
		mUpdatePanel.setVisibility(View.GONE);
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

}