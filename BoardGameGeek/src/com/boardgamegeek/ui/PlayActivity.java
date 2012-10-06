package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.database.PlayPersister;
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
	private static final String TAG = makeLogTag(PlayActivity.class);

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";

	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYER = 2;

	private static final int AGE_IN_DAYS_TO_REFRESH = 7;

	private LogInHelper mLogInHelper;
	private NotifyingAsyncQueryHandler mHandler;
	private Uri mPlayUri;
	private Uri mPlayerUri;
	private PlayObserver mObserver;

	private int mGameId;
	private String mGameName;
	private Play mPlay = new Play();

	private TextView mUpdated;
	private TextView mPlayId;
	private TextView mDate;
	private TextView mQuantity;
	private TextView mLength;
	private TextView mLocation;
	private View mIncomplete;
	private View mNoWinStats;
	private TextView mComments;
	private LinearLayout mPlayerList;
	private View mUpdatePanel;
	private View mUnsyncedView;
	private TextView mSavedTimeStamp;
	private TextView mUnsyncedMessage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		setUiVariables();
		mLogInHelper = new LogInHelper(this, this);
		parseIntent();
		UIUtils.setGameHeader(this, mGameName, mGameId);
		findViewById(R.id.game_thumbnail).setClickable(true);

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentResolver().registerContentObserver(mPlayUri, true, mObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		startQuery();
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

		if (mGameId == -1) {
			LOGW(TAG, "Didn't get a game ID");
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
		// TODO: always enable this?
		MenuItem mi = menu.findItem(R.id.menu_delete);
		mi.setEnabled(mLogInHelper.checkCookies());

		menu.findItem(R.id.menu_refresh).setVisible(mPlay.hasBeenSynced());
		menu.findItem(R.id.menu_share).setVisible(mPlay.hasBeenSynced());
		menu.findItem(R.id.menu_share).setEnabled(mPlay.SyncStatus == Play.SYNC_STATUS_SYNCED);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// TODO add menu_send if modified
			case R.id.menu_edit:
				ActivityUtils.logPlay(this, mPlay.PlayId, mGameId, mGameName);
				return true;
			case R.id.menu_delete: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.are_you_sure_title).setMessage(R.string.are_you_sure_delete_play)
					.setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							boolean deleted = false;
							if (mPlay.hasBeenSynced()) {
								// TODO: if unsuccessful, mark for deletion in DB?
								deleted = ActivityUtils.deletePlay(PlayActivity.this, mLogInHelper.getCookieStore(),
									mPlay.PlayId);
							} else {
								PlayPersister ph = new PlayPersister(getContentResolver(), mPlay);
								deleted = ph.delete();
							}
							if (deleted) {
								finish();
							}
						}
					}).setNegativeButton(R.string.no, null);
				builder.create().show();
				return true;
			}
			case R.id.menu_refresh: {
				if (mPlay.SyncStatus != Play.SYNC_STATUS_SYNCED) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.are_you_sure_title).setMessage(R.string.are_you_sure_refresh_message)
						.setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								mPlay.SyncStatus = Play.SYNC_STATUS_SYNCED;
								PlayPersister ph = new PlayPersister(getContentResolver(), mPlay);
								ph.save();
								refresh();
							}
						}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setResult(Activity.RESULT_CANCELED);
								dialog.cancel();
							}
						});
					builder.create().show();
				} else {
					refresh();
				}
				return true;
			}
			case R.id.menu_share:
				sharePlay();
				return true;
		}
		return false;
	}

	private void sharePlay() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, mPlay.toShortDescription(this));
		shareIntent.putExtra(Intent.EXTRA_TEXT, mPlay.toLongDescription(this));
		startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.share_play_title)));
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
		Intent intent = new Intent(Intent.ACTION_VIEW, Games.buildGameUri(mGameId));
		intent.putExtra(GameActivity.KEY_GAME_NAME, mGameName);
		startActivity(intent);
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

				mPlay.populate(cursor);
				bindUi();

				if (mPlay.hasBeenSynced()
					&& (mPlay.Updated == 0 || DateTimeUtils.howManyDaysOld(mPlay.Updated) > AGE_IN_DAYS_TO_REFRESH)) {
					refresh();
				}
			} else if (token == TOKEN_PLAYER) {
				mPlay.clearPlayers();
				while (cursor.moveToNext()) {
					Player player = new Player(cursor);
					mPlay.addPlayer(player);
				}
				bindUi();
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void setUiVariables() {
		mUpdated = (TextView) findViewById(R.id.updated);
		mPlayId = (TextView) findViewById(R.id.play_id);
		mDate = (TextView) findViewById(R.id.play_date);
		mQuantity = (TextView) findViewById(R.id.play_quantity);
		mLength = (TextView) findViewById(R.id.play_length);
		mLocation = (TextView) findViewById(R.id.play_location);
		mIncomplete = findViewById(R.id.play_incomplete);
		mNoWinStats = findViewById(R.id.play_no_win_stats);
		mComments = (TextView) findViewById(R.id.play_comments);
		mPlayerList = (LinearLayout) findViewById(R.id.play_player_list);
		mUpdatePanel = findViewById(R.id.update_panel);
		mUnsyncedView = findViewById(R.id.play_unsynced);
		mSavedTimeStamp = (TextView) findViewById(R.id.play_saved);
		mUnsyncedMessage = (TextView) findViewById(R.id.play_unsynced_message);
		mObserver = new PlayObserver(new Handler());
	}

	private synchronized void bindUi() {
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

		mPlayerList.removeAllViews();
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
			mUpdated.setText(getResources().getString(R.string.updated) + " "
				+ DateUtils.getRelativeTimeSpanString(updated, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
		}

		if (mPlay.hasBeenSynced()) {
			mPlayId.setText(String.format(getResources().getString(R.string.id_list_text), mPlay.PlayId));
		}

		if (mPlay.SyncStatus != Play.SYNC_STATUS_SYNCED) {
			mUnsyncedView.setVisibility(View.VISIBLE);
			mSavedTimeStamp.setText(getResources().getString(R.string.saved)
				+ " "
				+ DateUtils.getRelativeTimeSpanString(mPlay.Saved, System.currentTimeMillis(),
					DateUtils.MINUTE_IN_MILLIS));
			if (mPlay.SyncStatus == Play.SYNC_STATUS_IN_PROGRESS) {
				mUnsyncedMessage.setText(R.string.sync_in_process);
			} else if (mPlay.SyncStatus == Play.SYNC_STATUS_PENDING) {
				mUnsyncedMessage.setText(R.string.sync_pending);
			}
		} else {
			mUnsyncedView.setVisibility(View.GONE);
		}
	}

	private interface Query {
		String[] PROJECTION = { Plays.PLAY_ID, PlayItems.NAME, PlayItems.OBJECT_ID, Plays.DATE, Plays.LOCATION,
			Plays.LENGTH, Plays.QUANTITY, Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.COMMENTS, Plays.UPDATED_LIST,
			Plays.SYNC_STATUS, Plays.UPDATED };

		int NAME = 1;
	}

	private interface PlayerQuery {
		String[] PROJECTION = { PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.START_POSITION, PlayPlayers.COLOR,
			PlayPlayers.SCORE, PlayPlayers.RATING, PlayPlayers.NEW, PlayPlayers.WIN, };
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
			super.onChange(selfChange);
			startQuery();
		}
	}

	private void startQuery() {
		mHandler.startQuery(TOKEN_PLAY, mPlayUri, Query.PROJECTION);
		mHandler.startQuery(TOKEN_PLAYER, mPlayerUri, PlayerQuery.PROJECTION);
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
			LOGD(TAG, "Refreshing game ID [" + gameId + "]");
			final String url = HttpUtils.constructPlayUrlSpecific(gameId, null);
			try {
				// TODO track the play IDs for this game ID, removing any plays
				// not returned (hopefully can get an API change for this)
				RemotePlaysHandler handler = new RemotePlaysHandler();
				mExecutor.executeGet(url, handler);
			} catch (HandlerException e) {
				LOGE(TAG, "Exception trying to refresh game ID [" + gameId + "]", e);
				showToastOnUiThread(R.string.msg_update_error);
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			hideLoadingMessage();
			LOGD(TAG, "Refresh took " + (System.currentTimeMillis() - mStartTime) + "ms");
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