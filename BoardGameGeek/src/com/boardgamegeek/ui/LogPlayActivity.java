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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.LogInHelper;
import com.boardgamegeek.util.LogInHelper.LogInListener;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayActivity extends Activity implements LogInListener {
	private static final String TAG = "LogPlayActivity";

	private static final int DATE_DIALOG_ID = 0;
	private static final int LOGGING_DIALOG_ID = 1;
	private static final int REQUEST_ADD_PLAYER = 0;

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_LENGTH_SHOWN = "LENGTH_SHOWN";
	private static final String KEY_LOCATION_SHOWN = "LOCATION_SHOWN";
	private static final String KEY_INCOMPLETE_SHOWN = "INCOMPLETE_SHOWN";
	private static final String KEY_NO_WIN_STATS_SHOWN = "NO_WIN_STATS_SHOWN";
	private static final String KEY_COMMENTS_SHOWN = "COMMENTS_SHOWN";
	private static final String KEY_PLAYERS_SHOWN = "PLAYERS_SHOWN";

	private String mGameName;
	private String mThumbnailUrl;
	private Play mPlay;
	private int mNextPlayerTag = 1;

	private LogInHelper mLogInHelper;

	private Button mDateButton;
	private EditText mQuantityView;
	private EditText mLengthView;
	private EditText mLocationView;
	private CheckBox mIncompleteView;
	private CheckBox mNoWinStatsView;
	private EditText mCommentsView;
	private TextView mPlayerHeader;
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
		UIUtils.setTitle(this);
		setUiVariables();
		mLogInHelper = new LogInHelper(this, this);
		mCancelDialog = UIUtils.createCancelDialog(this);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			final int gameId = intent.getExtras().getInt(KEY_GAME_ID);
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
			if (gameId == -1) {
				Log.w(TAG, "Didn't get a game ID");
				finish();
			}

			mPlay = new Play(gameId);

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
			bindUi();
		}

		hideFields();

		UIUtils u = new UIUtils(this);
		u.setGameName(mGameName);
		u.setThumbnail(mThumbnailUrl);
		setDateButtonText();
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
				builder.setTitle("Add Field");
				builder.setItems(array, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Resources r = getResources();

						String selection = array[which].toString();
						if (selection == r.getString(R.string.length)) {
							mLengthShown = true;
							findViewById(R.id.log_length_row).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.location)) {
							mLocationShown = true;
							findViewById(R.id.log_location_row).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.incomplete)) {
							mIncompleteShown = true;
							findViewById(R.id.log_incomplete).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.noWinStats)) {
							mNoWinStatsShown = true;
							findViewById(R.id.log_no_win_stats).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.comments)) {
							mCommentsShown = true;
							findViewById(R.id.log_comments_label).setVisibility(View.VISIBLE);
							findViewById(R.id.log_comments).setVisibility(View.VISIBLE);
						} else if (selection == r.getString(R.string.players)) {
							mPlayersShown = true;
							findViewById(R.id.log_player_list_divider).setVisibility(View.VISIBLE);
							findViewById(R.id.log_player_list).setVisibility(View.VISIBLE);
						}
					}
				});
				builder.show();
				return true;
		}
		return false;
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
		mQuantityView.setText(String.valueOf(mPlay.Quantity));
		mLengthView.setText(String.valueOf(mPlay.Length));
		mLocationView.setText(mPlay.Location);
		mIncompleteView.setChecked(mPlay.Incomplete);
		mNoWinStatsView.setChecked(mPlay.NoWinStats);
		mCommentsView.setText(mPlay.Comments);
		for (Player player : mPlay.getPlayers()) {
			addPlayer(player);
		}
	}

	private void addPlayer(Player p) {
		PlayerRow pr = new PlayerRow(this);
		pr.setPlayer(p);
		pr.setTag(mNextPlayerTag++);
		pr.setOnEditListener(onPlayerEdit());
		pr.setOnDeleteListener(onPlayerDelete());
		mPlayerList.addView(pr, mPlayerList.getChildCount() - 1);
		displayPlayerCount();
	}

	private void displayPlayerCount() {
		Resources r = getResources();
		int playerCount = mPlayerList.getChildCount() - 4;
		if (playerCount <= 0) {
			mPlayerHeader.setText(r.getString(R.string.players));
		} else {
			mPlayerHeader.setText(String.valueOf(playerCount) + " " + r.getString(R.string.players));
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
		intent.setClass(LogPlayActivity.this, LogPlayerActivity.class);
		intent.putExtra(LogPlayerActivity.KEY_GAME_NAME, mGameName);
		intent.putExtra(LogPlayerActivity.KEY_THUMBNAIL_URL, mThumbnailUrl);
		startActivityForResult(intent, requestCode);
	}

	private void setUiVariables() {
		mDateButton = (Button) findViewById(R.id.logDateButton);
		mQuantityView = (EditText) findViewById(R.id.logQuantity);
		mLengthView = (EditText) findViewById(R.id.logLength);
		mLocationView = (EditText) findViewById(R.id.logLocation);
		mIncompleteView = (CheckBox) findViewById(R.id.log_incomplete);
		mNoWinStatsView = (CheckBox) findViewById(R.id.log_no_win_stats);
		mCommentsView = (EditText) findViewById(R.id.log_comments);
		mPlayerHeader = (TextView) findViewById(R.id.player_header);
		mPlayerList = (LinearLayout) findViewById(R.id.player_list);
		mSaveButton = (Button) findViewById(R.id.logPlaySaveButton);
	}

	private void hideFields() {
		if (hideLength()) {
			findViewById(R.id.log_length_row).setVisibility(View.GONE);
		}
		if (hideLocation()) {
			findViewById(R.id.log_location_row).setVisibility(View.GONE);
		}
		if (hideIncomplete()) {
			findViewById(R.id.log_incomplete).setVisibility(View.GONE);
		}
		if (hideNoWinStats()) {
			findViewById(R.id.log_no_win_stats).setVisibility(View.GONE);
		}
		if (hideComments()) {
			findViewById(R.id.log_comments_label).setVisibility(View.GONE);
			findViewById(R.id.log_comments).setVisibility(View.GONE);
		}
		if (hidePlayers()) {
			findViewById(R.id.log_player_list_divider).setVisibility(View.GONE);
			findViewById(R.id.log_player_list).setVisibility(View.GONE);
		}
	}

	private boolean hideLength() {
		return BggApplication.getInstance().getPlayLoggingHideLength() && !mLengthShown;
	}

	private boolean hideLocation() {
		return BggApplication.getInstance().getPlayLoggingHideLocation() && !mLocationShown;
	}

	private boolean hideIncomplete() {
		return BggApplication.getInstance().getPlayLoggingHideIncomplete() && !mIncompleteShown;
	}

	private boolean hideNoWinStats() {
		return BggApplication.getInstance().getPlayLoggingHideNoWinStats() && !mNoWinStatsShown;
	}

	private boolean hideComments() {
		return BggApplication.getInstance().getPlayLoggingHideComments() && !mCommentsShown;
	}

	private boolean hidePlayers() {
		return BggApplication.getInstance().getPlayLoggingHidePlayerList() && !mPlayersShown;
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

			// create form entity
			UrlEncodedFormEntity entity;
			List<NameValuePair> nvps = mPlay.toNameValuePairs();
			try {
				entity = new UrlEncodedFormEntity(nvps, HTTP.ASCII);
			} catch (UnsupportedEncodingException e) {
				return e.toString();
			}

			final HttpClient client = HttpUtils.createHttpClient(LogPlayActivity.this, mLogInHelper.getCookieStore());

			final HttpPost post = new HttpPost(BggApplication.siteUrl + "geekplay.php");
			post.setEntity(entity);

			HttpResponse response = null;
			try {
				response = client.execute(post);
				if (response == null) {
					return r.getString(R.string.logInError) + " : " + r.getString(R.string.logInErrorSuffixNoResponse);
				}
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					return r.getString(R.string.logInError) + " : " + r.getString(R.string.logInErrorSuffixBadResponse)
							+ " " + response.toString() + ".";
				}
				return HttpUtils.parseResponse(response);
			} catch (ClientProtocolException e) {
				return e.toString();
			} catch (IOException e) {
				return e.toString();
			} finally {
				if (client != null && client.getConnectionManager() != null) {
					client.getConnectionManager().shutdown();
				}
			}
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d(TAG, "play result: " + result);
			removeDialog(LOGGING_DIALOG_ID);
			if (isValidResponse(result)) {
				final int playCount = parsePlayCount(result);
				final String countDescription = getPlayCountDescription(playCount);
				showToast(String.format(r.getString(R.string.logPlaySuccess), countDescription, mGameName));
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
			mPlay.setDate(year, monthOfYear, dayOfMonth);
			setDateButtonText();
		}
	};

	private void setDateButtonText() {
		mDateButton.setText(mPlay.getDateText());
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
		// date info already captured
		mPlay.Quantity = StringUtils.parseInt(mQuantityView.getText().toString(), 1);
		mPlay.Length = StringUtils.parseInt(mLengthView.getText().toString(), 0);
		mPlay.Location = mLocationView.getText().toString();
		mPlay.Incomplete = mIncompleteView.isChecked();
		mPlay.NoWinStats = mNoWinStatsView.isChecked();
		mPlay.Comments = mCommentsView.getText().toString();
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
}
