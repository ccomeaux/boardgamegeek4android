package com.boardgamegeek.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
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
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.HttpUtils;
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

	private static final int TOKEN_PLAY = 1;
	private static final int TOKEN_PLAYER = 2;
	private static final int TOKEN_GAME = 3;

	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_LENGTH_SHOWN = "LENGTH_SHOWN";
	private static final String KEY_LOCATION_SHOWN = "LOCATION_SHOWN";
	private static final String KEY_INCOMPLETE_SHOWN = "INCOMPLETE_SHOWN";
	private static final String KEY_NO_WIN_STATS_SHOWN = "NO_WIN_STATS_SHOWN";
	private static final String KEY_COMMENTS_SHOWN = "COMMENTS_SHOWN";
	private static final String KEY_PLAYERS_SHOWN = "PLAYERS_SHOWN";

	private NotifyingAsyncQueryHandler mHandler;
	private Uri mPlayUri;
	private Uri mPlayerUri;
	private LocationAdapter mLocationAdapter;

	private String mGameName;
	private String mThumbnailUrl;
	private Play mPlay;
	private int mNextPlayerTag = 1;

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
	private Button mSaveButton;
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

			if (gameId == -1 && playId <= 0) {
				Log.w(TAG, "Didn't get a game ID or play ID");
				finish();
			}

			if (playId > 0) {
				mPlay = new Play();

				mPlayUri = Plays.buildPlayUri(playId);
				mPlayerUri = Plays.buildPlayerUri(playId);

				mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
				mHandler.startQuery(TOKEN_PLAY, mPlayUri, Query.PROJECTION);
				mHandler.startQuery(TOKEN_PLAYER, mPlayerUri, PlayerQuery.PROJECTION);
				if (TextUtils.isEmpty(mThumbnailUrl) && gameId > 0) {
					Uri uri = Games.buildGameUri(gameId);
					mHandler.startQuery(TOKEN_GAME, uri, GameQuery.PROJECTION);
				}
			} else {
				mPlay = new Play(gameId);
			}

			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				quickLogPlay();
				finish();
			} else if (!Intent.ACTION_EDIT.equals(intent.getAction())) {
				Log.w(TAG, "Received bad intent action: " + intent.getAction());
				finish();
			}
		} else {
			mPlay = new Play(savedInstanceState);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mLengthShown = savedInstanceState.getBoolean(KEY_LENGTH_SHOWN);
			mLocationShown = savedInstanceState.getBoolean(KEY_LOCATION_SHOWN);
			mIncompleteShown = savedInstanceState.getBoolean(KEY_INCOMPLETE_SHOWN);
			mNoWinStatsShown = savedInstanceState.getBoolean(KEY_NO_WIN_STATS_SHOWN);
			mCommentsShown = savedInstanceState.getBoolean(KEY_COMMENTS_SHOWN);
			mPlayersShown = savedInstanceState.getBoolean(KEY_PLAYERS_SHOWN);
		}

		bindUi();

		UIUtils.setGameHeader(this, mGameName, mThumbnailUrl);
		setDateButtonText();

		UIUtils.showHelpDialog(this, BggApplication.HELP_LOGPLAY_KEY, HELP_VERSION, R.string.help_logplay);

		mLocationAdapter = new LocationAdapter(this);
		mLocationView.setAdapter(mLocationAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mLogInHelper.logIn();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		captureForm();
		mPlay.saveState(outState);
		outState.putString(KEY_GAME_NAME, mGameName);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putBoolean(KEY_LENGTH_SHOWN, mLengthShown);
		outState.putBoolean(KEY_LOCATION_SHOWN, mLocationShown);
		outState.putBoolean(KEY_INCOMPLETE_SHOWN, mIncompleteShown);
		outState.putBoolean(KEY_NO_WIN_STATS_SHOWN, mNoWinStatsShown);
		outState.putBoolean(KEY_COMMENTS_SHOWN, mCommentsShown);
		outState.putBoolean(KEY_PLAYERS_SHOWN, mPlayersShown);
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
		MenuItem mi = menu.findItem(R.id.save);
		mi.setEnabled(mSaveButton.isEnabled());

		mi = menu.findItem(R.id.add_field);
		mi.setEnabled(hideLength() || hideLocation() || hideNoWinStats() || hideIncomplete() || hideComments()
				|| hidePlayers());

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.save:
				logPlay();
				return true;
			case R.id.cancel:
				cancel();
				return true;
			case R.id.add_field:
				final CharSequence[] array = createAddFieldArray();
				if (array == null || array.length == 0) {
					return false;
				}

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
				return true;
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			cancel();
			return true;
		}
		return super.onKeyUp(keyCode, event);
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

		CharSequence[] csa = {};
		csa = list.toArray(csa);
		return csa;
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

	public void onSaveClick(View v) {
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
			cursor.close();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Player p = new Player(data);
			if (requestCode == REQUEST_ADD_PLAYER) {
				addPlayer(p);
			} else {
				PlayerRow pr = (PlayerRow) mPlayerList.findViewWithTag(requestCode);
				pr.setPlayer(p);
			}
		}
	}

	private void bindUi() {
		if (mPlay != null) {
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
		} else {
			mQuantityView.setText("1");
		}
	}

	private void addPlayer(Player p) {
		PlayerRow pr = new PlayerRow(this);
		pr.setPlayer(p);
		pr.setTag(mNextPlayerTag++);
		pr.setOnEditListener(onPlayerEdit());
		pr.setOnDeleteListener(onPlayerDelete());
		mPlayerList.addView(pr, mPlayerList.getChildCount() - 2);
		displayPlayerCount();
	}

	private void displayPlayerCount() {
		Resources r = getResources();
		int playerCount = mPlayerList.getChildCount() - 14;
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
				displayPlayerCount();
			}
		};
	}

	private void addPlayer(Intent intent, int requestCode) {
		if (mPlay == null) {
			Toast.makeText(this, "Can't add player, error initializing.", Toast.LENGTH_LONG).show();
			return;
		}
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
		mSaveButton = (Button) findViewById(R.id.log_play_save);
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
		if (mPlay == null) {
			Toast.makeText(this, "Can't log play, error initializing.", Toast.LENGTH_LONG).show();
			return;
		}
		captureForm();
		LogPlayTask task = new LogPlayTask();
		task.execute(mPlay);
	}

	private void cancel() {
		mCancelDialog.show();
	}

	class LogPlayTask extends AsyncTask<Play, Void, String> {
		Play mPlay;
		Resources r;

		LogPlayTask() {
			r = getResources();
		}

		@Override
		protected void onPreExecute() {
			showDialog(LOGGING_DIALOG_ID);
		}

		@Override
		protected String doInBackground(Play... params) {
			mPlay = params[0];

			// update colors
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

			// create form entity
			UrlEncodedFormEntity entity;
			List<NameValuePair> nvps = mPlay.toNameValuePairs();
			try {
				entity = new UrlEncodedFormEntity(nvps, HTTP.UTF_8);
			} catch (UnsupportedEncodingException e) {
				return e.toString();
			}

			final HttpClient client = HttpUtils.createHttpClient(LogPlayActivity.this, mLogInHelper.getCookieStore());

			final HttpPost post = new HttpPost(BggApplication.siteUrl + "geekplay.php");
			post.setEntity(entity);

			String message = "";
			HttpResponse response = null;
			try {
				response = client.execute(post);
				if (response == null) {
					message = r.getString(R.string.logInError) + " : "
							+ r.getString(R.string.logInErrorSuffixNoResponse);
				} else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					message = r.getString(R.string.logInError) + " : "
							+ r.getString(R.string.logInErrorSuffixBadResponse) + " " + response.toString() + ".";
				} else {
					message = HttpUtils.parseResponse(response);
					if (isValidResponse(message)) {
						// Sync the new game back locally
						RemoteExecutor re = new RemoteExecutor(client, getContentResolver());
						re.executeGet(HttpUtils.constructPlayUrlSpecific(mPlay.GameId, mPlay.getFormattedDate()),
								new RemotePlaysHandler());
					}
				}
			} catch (ClientProtocolException e) {
				message = e.toString();
			} catch (IOException e) {
				message = e.toString();
			} finally {
				if (client != null && client.getConnectionManager() != null) {
					client.getConnectionManager().shutdown();
				}
			}

			return message;
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d(TAG, "play result: " + result);
			removeDialog(LOGGING_DIALOG_ID);
			if (isValidResponse(result)) {
				String message = r.getString(R.string.msg_play_updated);
				if (mPlay.PlayId <= 0) {
					int playCount = parsePlayCount(result);
					String countDescription = getPlayCountDescription(playCount);
					message = String.format(r.getString(R.string.logPlaySuccess), countDescription, mGameName);
				}
				showToast(message);
				finish();
			} else {
				showToast(result);
			}
		}

		private boolean isValidResponse(String result) {
			if (TextUtils.isEmpty(result)) {
				return false;
			}
			return result.startsWith("Plays: <a") || result.startsWith("{\"html\":\"Plays:");
		}

		private int parsePlayCount(String result) {
			int start = result.indexOf(">");
			int end = result.indexOf("<", start);
			int playCount = StringUtils.parseInt(result.substring(start + 1, end), 1);
			return playCount;
		}

		private String getPlayCountDescription(int playCount) {
			String countDescription = "";
			int quantity = mPlay.Quantity;
			switch (quantity) {
				case 1:
					countDescription = StringUtils.getOrdinal(playCount);
					break;
				case 2:
					countDescription = StringUtils.getOrdinal(playCount - 1) + " & "
							+ StringUtils.getOrdinal(playCount);
					break;
				default:
					countDescription = StringUtils.getOrdinal(playCount - quantity + 1) + " - "
							+ StringUtils.getOrdinal(playCount);
					break;
			}
			return countDescription;
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
		if (mPlay != null) {
			mDateButton.setText(mPlay.getDateText());
		}
	}

	private void enableSave() {
		mSaveButton.setEnabled(true);
	}

	@Override
	public void onLogInSuccess() {
		enableSave();
	}

	@Override
	public void onLogInError(String errorMessage) {
		Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onNeedCredentials() {
		Toast.makeText(this, R.string.setUsernamePassword, Toast.LENGTH_LONG).show();
		startActivity(new Intent(this, Preferences.class));
		finish();
	}

	private void captureForm() {
		if (mPlay == null) {
			Toast.makeText(this, "Can't save, error initializing form.", Toast.LENGTH_LONG).show();
			return;
		}
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
			if (!TextUtils.isEmpty(constraint)) {
				selection = Plays.LOCATION + " LIKE '" + constraint + "%'";
			}
			return getContentResolver().query(Plays.buildLocationsUri(), LocationQuery.PROJECTION, selection, null,
					null);
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
