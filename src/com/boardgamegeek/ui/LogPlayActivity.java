package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.database.PlayHelper;
import com.boardgamegeek.io.PlaySender;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.LogInHelper;
import com.boardgamegeek.util.LogInHelper.LogInListener;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayActivity extends Activity implements LogInListener, AsyncQueryListener {
	private static final String TAG = "LogPlayActivity";

	private static final int HELP_VERSION = 1;
	private static final int DATE_DIALOG_ID = 0;
	private static final int LOGGING_DIALOG_ID = 1;
	private static final int REQUEST_ADD_PLAYER = 0;
	private static final int NOTIFICATION_ID = 2;

	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYER = 2;
	private static final int TOKEN_GAME = 3;

	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_START_TIME = "START_TIME";
	private static final String KEY_LENGTH_SHOWN = "LENGTH_SHOWN";
	private static final String KEY_LOCATION_SHOWN = "LOCATION_SHOWN";
	private static final String KEY_INCOMPLETE_SHOWN = "INCOMPLETE_SHOWN";
	private static final String KEY_NO_WIN_STATS_SHOWN = "NO_WIN_STATS_SHOWN";
	private static final String KEY_COMMENTS_SHOWN = "COMMENTS_SHOWN";
	private static final String KEY_PLAYERS_SHOWN = "PLAYERS_SHOWN";

	private NotifyingAsyncQueryHandler mHandler;
	private LocationAdapter mLocationAdapter;

	private String mGameName;
	private String mThumbnailUrl;
	private Play mPlay;
	private int mNextPlayerTag = 1;
	private boolean mLaunchingActivity;
	private long mStartTime;

	private LogInHelper mLogInHelper;

	private Button mDateButton;
	private EditText mQuantityView;
	private EditText mLengthView;
	private AutoCompleteTextView mLocationView;
	private CheckBox mIncompleteView;
	private CheckBox mNoWinStatsView;
	private EditText mCommentsView;
	private TextView mPlayerLabel;
	private LinearLayout mPlayerList;
	private Button mSendButton;
	private AlertDialog mCancelDialog;

	private boolean mLengthShown;
	private boolean mLocationShown;
	private boolean mIncompleteShown;
	private boolean mNoWinStatsShown;
	private boolean mCommentsShown;
	private boolean mPlayersShown;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplay);
		setUiVariables();
		mLogInHelper = new LogInHelper(this, this);
		mCancelDialog = UIUtils.createCancelDialog(this);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();

			int playId = intent.getExtras().getInt(KEY_PLAY_ID);
			int gameId = intent.getExtras().getInt(KEY_GAME_ID);
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
			mStartTime = intent.getExtras().getLong(KEY_START_TIME);

			if (gameId <= 0) {
				Log.w(TAG, "Didn't get a game ID");
				finish();
			}

			mPlay = new Play(gameId, mGameName);
			if (playId > 0) {
				startQuery(playId, gameId);
			}

			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				quickLogPlay();
				finish();
			} else if (!Intent.ACTION_EDIT.equals(intent.getAction())) {
				Log.w(TAG, "Received bad intent action: " + intent.getAction());
				finish();
			}
		} else {
			restoreInstanceState(savedInstanceState);
		}

		bindUi();

		UIUtils.setGameHeader(this, mGameName, mThumbnailUrl);
		setDateButtonText();

		UIUtils.showHelpDialog(this, BggApplication.HELP_LOGPLAY_KEY, HELP_VERSION, R.string.help_logplay);

		mLocationAdapter = new LocationAdapter(this);
		mLocationView.setAdapter(mLocationAdapter);
	}

	private void startQuery(int playId, int gameId) {
		if (mHandler == null) {
			mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		}

		mHandler.startQuery(TOKEN_PLAY, Plays.buildPlayUri(playId), Query.PROJECTION);
		mHandler.startQuery(TOKEN_PLAYER, Plays.buildPlayerUri(playId), PlayerQuery.PROJECTION);
		if (TextUtils.isEmpty(mThumbnailUrl) && gameId > 0) {
			Uri uri = Games.buildGameUri(gameId);
			mHandler.startQuery(TOKEN_GAME, uri, GameQuery.PROJECTION);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mLaunchingActivity = false;
		mLogInHelper.logIn();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		save();
		mPlay.saveState(outState);
		outState.putString(KEY_GAME_NAME, mGameName);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putLong(KEY_START_TIME, mStartTime);
		outState.putBoolean(KEY_LENGTH_SHOWN, mLengthShown);
		outState.putBoolean(KEY_LOCATION_SHOWN, mLocationShown);
		outState.putBoolean(KEY_INCOMPLETE_SHOWN, mIncompleteShown);
		outState.putBoolean(KEY_NO_WIN_STATS_SHOWN, mNoWinStatsShown);
		outState.putBoolean(KEY_COMMENTS_SHOWN, mCommentsShown);
		outState.putBoolean(KEY_PLAYERS_SHOWN, mPlayersShown);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		mPlay = new Play(savedInstanceState);
		mGameName = savedInstanceState.getString(KEY_GAME_NAME);
		mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
		mStartTime = savedInstanceState.getLong(KEY_START_TIME);
		mLengthShown = savedInstanceState.getBoolean(KEY_LENGTH_SHOWN);
		mLocationShown = savedInstanceState.getBoolean(KEY_LOCATION_SHOWN);
		mIncompleteShown = savedInstanceState.getBoolean(KEY_INCOMPLETE_SHOWN);
		mNoWinStatsShown = savedInstanceState.getBoolean(KEY_NO_WIN_STATS_SHOWN);
		mCommentsShown = savedInstanceState.getBoolean(KEY_COMMENTS_SHOWN);
		mPlayersShown = savedInstanceState.getBoolean(KEY_PLAYERS_SHOWN);
	}

	@Override
	protected void onPause() {
		if (!isFinishing() && !mLaunchingActivity) {
			save();
		}
		super.onPause();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DATE_DIALOG_ID:
				return new DatePickerDialog(this, mDateSetListener, mPlay.Year, mPlay.Month, mPlay.Day);
			case LOGGING_DIALOG_ID:
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setTitle(R.string.logPlayDialogTitle);
				dialog.setMessage(getResources().getString(R.string.logPlayDialogMessage));
				dialog.setIndeterminate(true);
				dialog.setCancelable(true);
				return dialog;
		}
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.logplay, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_send).setEnabled(mSendButton.isEnabled());
		menu.findItem(R.id.menu_start).setVisible(!mPlay.hasEnded());
		menu.findItem(R.id.menu_add_field).setEnabled(
				hideLength() || hideLocation() || hideNoWinStats() || hideIncomplete() || hideComments()
						|| hidePlayers());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_send:
				logPlay();
				return true;
			case R.id.menu_save:
				save();
				finish();
				return true;
			case R.id.menu_start:
				startPlay();
				return true;
			case R.id.menu_cancel:
				cancel();
				return true;
			case R.id.menu_add_field:
				final CharSequence[] array = createAddFieldArray();
				if (array == null || array.length == 0) {
					return false;
				}
				promptAddField(array);
				return true;
		}
		return false;
	}

	private void promptAddField(final CharSequence[] array) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.add_field);
		builder.setItems(array, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Resources r = getResources();

				String selection = array[which].toString();
				if (selection == r.getString(R.string.length)) {
					mLengthShown = true;
					findViewById(R.id.log_play_length_label).setVisibility(View.VISIBLE);
					mLengthView.setVisibility(View.VISIBLE);
				} else if (selection == r.getString(R.string.location)) {
					mLocationShown = true;
					findViewById(R.id.log_play_location_label).setVisibility(View.VISIBLE);
					mLocationView.setVisibility(View.VISIBLE);
				} else if (selection == r.getString(R.string.incomplete)) {
					mIncompleteShown = true;
					findViewById(R.id.log_play_incomplete).setVisibility(View.VISIBLE);
				} else if (selection == r.getString(R.string.noWinStats)) {
					mNoWinStatsShown = true;
					findViewById(R.id.log_play_no_win_stats).setVisibility(View.VISIBLE);
				} else if (selection == r.getString(R.string.comments)) {
					mCommentsShown = true;
					findViewById(R.id.log_play_comments_label).setVisibility(View.VISIBLE);
					findViewById(R.id.log_play_comments).setVisibility(View.VISIBLE);
				} else if (selection == r.getString(R.string.players)) {
					mPlayersShown = true;
					mPlayerLabel.setVisibility(View.VISIBLE);
					findViewById(R.id.log_play_players_add).setVisibility(View.VISIBLE);
				}
			}
		});
		builder.show();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO replace with onBackPressed in API 5
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			cancel();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private void save() {
		captureForm();
		mPlay.SyncStatus = Play.SYNC_STATUS_IN_PROGRESS;
		new PlayHelper(getContentResolver(), mPlay).save();
	}

	private void startPlay() {
		save();
		Intent intent = createIntent(true);
		launchStartNotification(intent);
	}

	private Intent createIntent(boolean includeStartTime) {
		Intent intent = new Intent(this, LogPlayActivity.class);
		intent.setAction(Intent.ACTION_EDIT);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(KEY_PLAY_ID, mPlay.PlayId);
		intent.putExtra(KEY_GAME_ID, mPlay.GameId);
		intent.putExtra(KEY_GAME_NAME, mGameName);
		intent.putExtra(KEY_THUMBNAIL_URL, mThumbnailUrl);
		if (includeStartTime) {
			intent.putExtra(KEY_START_TIME, System.currentTimeMillis());
		}
		return intent;
	}

	private void launchStartNotification(Intent intent) {
		String title = getResources().getString(R.string.notification_title);
		String message = String.format(getResources().getString(R.string.notification_text_playing), mGameName);
		Notification notification = new Notification(R.drawable.ic_stat_play, message, System.currentTimeMillis());
		notification.setLatestEventInfo(this, title, message,
				PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);
	}

	private CharSequence[] createAddFieldArray() {
		Resources r = getResources();
		List<CharSequence> list = new ArrayList<CharSequence>();

		if (hideLength()) {
			list.add(r.getString(R.string.length));
		}
		if (hideLocation()) {
			list.add(r.getString(R.string.location));
		}
		if (hideIncomplete()) {
			list.add(r.getString(R.string.incomplete));
		}
		if (hideNoWinStats()) {
			list.add(r.getString(R.string.noWinStats));
		}
		if (hideComments()) {
			list.add(r.getString(R.string.comments));
		}
		if (hidePlayers()) {
			list.add(r.getString(R.string.players));
		}

		CharSequence[] array = {};
		array = list.toArray(array);
		return array;
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	public void onDateClick(View v) {
		showDialog(DATE_DIALOG_ID);
	}

	public void onAddPlayerClick(View v) {
		if (BggApplication.getInstance().getPlayLoggingEditPlayer()) {
			addPlayer(new Intent(), REQUEST_ADD_PLAYER);
		} else {
			addPlayer(new Player());
		}
	}

	public void onSendClick(View v) {
		logPlay();
	}

	public void onCancelClick(View v) {
		cancel();
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
				if (mStartTime > 0) {
					mPlay.Length = DateTimeUtils.howManyMinutesOld(mStartTime);
				}
				bindUi();
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
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Player player = new Player(data);
			if (requestCode == REQUEST_ADD_PLAYER) {
				addPlayer(player);
			} else {
				((PlayerRow) mPlayerList.findViewWithTag(requestCode)).setPlayer(player);
			}
		}
	}

	private void bindUi() {
		setDateButtonText();
		mQuantityView.setText(String.valueOf(mPlay.Quantity));
		mLengthView.setText(String.valueOf(mPlay.Length));
		mLocationView.setText(mPlay.Location);
		mIncompleteView.setChecked(mPlay.Incomplete);
		mNoWinStatsView.setChecked(mPlay.NoWinStats);
		mCommentsView.setText(mPlay.Comments);
		for (Player player : mPlay.getPlayers()) {
			addPlayer(player);
		}
		hideFields();
	}

	private void addPlayer(Player p) {
		PlayerRow row = new PlayerRow(this);
		row.setPlayer(p);
		row.setTag(mNextPlayerTag++);
		row.setOnEditListener(onPlayerEdit());
		row.setOnDeleteListener(onPlayerDelete());
		mPlayerList.addView(row, mPlayerList.getChildCount() - 2);
		calculatePlayerCount();
	}

	private void calculatePlayerCount() {
		Resources r = getResources();
		int playerCount = mPlay.getPlayers().size();
		if (playerCount <= 0) {
			mPlayerLabel.setText(r.getString(R.string.players));
		} else {
			mPlayerLabel.setText(r.getString(R.string.players) + " - " + String.valueOf(playerCount));
		}
	}

	private OnClickListener onPlayerEdit() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayerRow row = (PlayerRow) v;
				Player player = row.getPlayer();
				addPlayer(player.toIntent(), (Integer) row.getTag());
			}
		};
	}

	private OnClickListener onPlayerDelete() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayerList.removeView(v);
				Toast.makeText(LogPlayActivity.this, R.string.msg_player_deleted, Toast.LENGTH_SHORT).show();
				calculatePlayerCount();
			}
		};
	}

	private void addPlayer(Intent intent, int requestCode) {
		mLaunchingActivity = true;
		intent.setClass(LogPlayActivity.this, LogPlayerActivity.class);
		intent.putExtra(LogPlayerActivity.KEY_GAME_ID, mPlay.GameId);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, mGameName);
		intent.putExtra(LogPlayerActivity.KEY_THUMBNAIL_URL, mThumbnailUrl);
		startActivityForResult(intent, requestCode);
	}

	private void setUiVariables() {
		mDateButton = (Button) findViewById(R.id.log_play_date);
		mQuantityView = (EditText) findViewById(R.id.log_play_quantity);
		mLengthView = (EditText) findViewById(R.id.log_play_length);
		mLocationView = (AutoCompleteTextView) findViewById(R.id.log_play_location);
		mIncompleteView = (CheckBox) findViewById(R.id.log_play_incomplete);
		mNoWinStatsView = (CheckBox) findViewById(R.id.log_play_no_win_stats);
		mCommentsView = (EditText) findViewById(R.id.log_play_comments);
		mPlayerLabel = (TextView) findViewById(R.id.log_play_players_label);
		mPlayerList = (LinearLayout) findViewById(R.id.log_play_player_list);
		mSendButton = (Button) findViewById(R.id.log_play_send);
	}

	private void hideFields() {
		findViewById(R.id.log_play_length_label).setVisibility(hideLength() ? View.GONE : View.VISIBLE);
		mLengthView.setVisibility(hideLength() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_location_label).setVisibility(hideLocation() ? View.GONE : View.VISIBLE);
		mLocationView.setVisibility(hideLocation() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_incomplete).setVisibility(hideIncomplete() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_no_win_stats).setVisibility(hideNoWinStats() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_comments_label).setVisibility(hideComments() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_comments).setVisibility(hideComments() ? View.GONE : View.VISIBLE);
		mPlayerLabel.setVisibility(hidePlayers() ? View.GONE : View.VISIBLE);
		findViewById(R.id.log_play_players_add).setVisibility(hidePlayers() ? View.GONE : View.VISIBLE);
	}

	private boolean hideLength() {
		return BggApplication.getInstance().getPlayLoggingHideLength() && !mLengthShown && !(mPlay.Length > 0);
	}

	private boolean hideLocation() {
		return BggApplication.getInstance().getPlayLoggingHideLocation() && !mLocationShown
				&& TextUtils.isEmpty(mPlay.Location);
	}

	private boolean hideIncomplete() {
		return BggApplication.getInstance().getPlayLoggingHideIncomplete() && !mIncompleteShown && !mPlay.Incomplete;
	}

	private boolean hideNoWinStats() {
		return BggApplication.getInstance().getPlayLoggingHideNoWinStats() && !mNoWinStatsShown && !mPlay.NoWinStats;
	}

	private boolean hideComments() {
		return BggApplication.getInstance().getPlayLoggingHideComments() && !mCommentsShown
				&& TextUtils.isEmpty(mPlay.Comments);
	}

	private boolean hidePlayers() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerList() && !mPlayersShown
				&& (mPlay.getPlayers().size() == 0);
	}

	private void quickLogPlay() {
		if (mLogInHelper.checkCookies()) {
			logPlay();
		} else {
			Toast.makeText(this, R.string.logInError, Toast.LENGTH_LONG).show();
		}
	}

	private void logPlay() {
		captureForm();
		LogPlayTask task = new LogPlayTask();
		task.execute(mPlay);
	}

	private void cancel() {
		mCancelDialog.show();
	}

	class LogPlayTask extends AsyncTask<Play, Void, PlaySender.Result> {
		Play mPlay;

		@Override
		protected void onPreExecute() {
			showDialog(LOGGING_DIALOG_ID);
		}

		@Override
		protected PlaySender.Result doInBackground(Play... params) {
			mPlay = params[0];

			updateColors();
			updateBuddyNicknames();
			return new PlaySender(LogPlayActivity.this, mLogInHelper.getCookieStore()).sendPlay(mPlay);
		}

		private void updateColors() {
			if (mPlay.getPlayers().size() > 0) {
				List<ContentValues> values = new ArrayList<ContentValues>();
				for (Player player : mPlay.getPlayers()) {
					String color = player.TeamColor;
					if (!TextUtils.isEmpty(color)) {
						ContentValues cv = new ContentValues();
						cv.put(GameColors.COLOR, player.TeamColor);
						values.add(cv);
					}
				}
				if (values.size() > 0) {
					ContentValues[] array = {};
					getContentResolver().bulkInsert(Games.buildColorsUri(mPlay.GameId), values.toArray(array));
				}
			}
		}

		private void updateBuddyNicknames() {
			if (mPlay.getPlayers().size() > 0) {
				for (Player player : mPlay.getPlayers()) {
					if (!TextUtils.isEmpty(player.Username) && !TextUtils.isEmpty(player.Name)) {
						ContentValues values = new ContentValues();
						values.put(Buddies.PLAY_NICKNAME, player.Name);
						getContentResolver().update(Buddies.CONTENT_URI, values, Buddies.BUDDY_NAME + "=?",
								new String[] { player.Username });
					}
				}
			}
		}

		@Override
		protected void onPostExecute(PlaySender.Result result) {
			Log.d(TAG, "play result: " + result);
			removeDialog(LOGGING_DIALOG_ID);
			if (result.isValidResponse()) {
				String message = getResources().getString(R.string.msg_play_updated);
				if (!mPlay.hasBeenSynced()) {
					String countDescription = result.getPlayCountDescription();
					message = String.format(getResources().getString(R.string.logPlaySuccess), countDescription,
							mGameName);
				}
				showToast(message);
			} else {
				showToast(result.ErrorMessage);
			}
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
			finish();
		}

		private void showToast(String result) {
			if (!TextUtils.isEmpty(result)) {
				Toast.makeText(LogPlayActivity.this, result, Toast.LENGTH_LONG).show();
			}
		}
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			if (mPlay != null) {
				mPlay.setDate(year, monthOfYear, dayOfMonth);
				setDateButtonText();
			}
		}
	};

	private void setDateButtonText() {
		mDateButton.setText(mPlay.getDateText());
	}

	private void enableSend() {
		mSendButton.setEnabled(true);
	}

	@Override
	public void onLogInSuccess() {
		enableSend();
	}

	@Override
	public void onLogInError(String errorMessage) {
		Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onNeedCredentials() {
		Toast.makeText(this, R.string.setUsernamePassword, Toast.LENGTH_LONG).show();
		mLaunchingActivity = true;
		startActivity(new Intent(this, Preferences.class));
		finish();
	}

	private void captureForm() {
		// date info already captured
		mPlay.Quantity = StringUtils.parseInt(mQuantityView.getText().toString().trim(), 1);
		mPlay.Length = StringUtils.parseInt(mLengthView.getText().toString().trim());
		mPlay.Location = mLocationView.getText().toString().trim();
		mPlay.Incomplete = mIncompleteView.isChecked();
		mPlay.NoWinStats = mNoWinStatsView.isChecked();
		mPlay.Comments = mCommentsView.getText().toString().trim();
		mPlay.clearPlayers();
		for (int i = 0; i < mPlayerList.getChildCount(); i++) {
			View view = mPlayerList.getChildAt(i);
			if (view instanceof PlayerRow) {
				PlayerRow pr = (PlayerRow) view;
				Player p = pr.getPlayer();
				mPlay.addPlayer(p);
			}
		}
	}

	private class LocationAdapter extends CursorAdapter {
		public LocationAdapter(Context context) {
			super(context, null);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return getLayoutInflater().inflate(R.layout.autocomplete_color, parent, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final TextView textView = (TextView) view.findViewById(R.id.autocomplete_color);
			textView.setText(cursor.getString(LocationQuery.LOCATION));
		}

		@Override
		public CharSequence convertToString(Cursor cursor) {
			return cursor.getString(LocationQuery.LOCATION);
		}

		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
			String selection = null;
			String[] selectionArgs = null;
			if (!TextUtils.isEmpty(constraint)) {
				selection = Plays.LOCATION + " LIKE ?";
				selectionArgs = new String[] { constraint + "%" };
			}
			return getContentResolver().query(Plays.buildLocationsUri(), LocationQuery.PROJECTION, selection,
					selectionArgs, null);
		}
	}

	private interface Query {
		String[] PROJECTION = { Plays.PLAY_ID, PlayItems.NAME, PlayItems.OBJECT_ID, Plays.DATE, Plays.LOCATION,
				Plays.LENGTH, Plays.QUANTITY, Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.COMMENTS, };

		int NAME = 1;
	}

	private interface PlayerQuery {
		String[] PROJECTION = { PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.START_POSITION, PlayPlayers.COLOR,
				PlayPlayers.SCORE, PlayPlayers.RATING, PlayPlayers.NEW, PlayPlayers.WIN, };
	}

	private interface GameQuery {
		String[] PROJECTION = { Games.THUMBNAIL_URL };

		int THUMBNAIL_URL = 0;
	}

	private interface LocationQuery {
		String[] PROJECTION = { Plays._ID, Plays.LOCATION };

		int LOCATION = 1;
	}
}
