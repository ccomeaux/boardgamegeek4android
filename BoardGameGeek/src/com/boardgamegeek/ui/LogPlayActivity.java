package com.boardgamegeek.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.ui.widget.BezelImageView;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.LogInHelper;
import com.boardgamegeek.util.LogInHelper.LogInListener;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;

public class LogPlayActivity extends Activity implements LogInListener {
	private static final String TAG = "LogPlayActivity";

	private static final int DATE_DIALOG_ID = 0;
	private static final int LOGGING_DIALOG_ID = 1;

	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";

	private static final String yearKey = "YEAR";
	private static final String monthKey = "MONTH";
	private static final String dayKey = "DAY";
	private static final String quantityKey = "QUANTITY";
	private static final String lengthKey = "LENGTH";
	private static final String locationKey = "LOCATION";
	private static final String incompleteKey = "INCOMPLETE";
	private static final String noWinStatsKey = "NO_WIN_STATS";
	private static final String commentsKey = "COMMENTS";

	private int mGameId;
	private String mGameName;
	private String mThumbnailUrl;

	private LogInHelper mLogInHelper;
	private DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);

	private int mYear;
	private int mMonth;
	private int mDay;

	private BezelImageView mThumbnail;
	private Button mDateButton;
	private EditText mQuantityView;
	private EditText mLengthView;
	private EditText mLocationView;
	private CheckBox mIncompleteView;
	private CheckBox mNoWinStatsView;
	private EditText mCommentsView;
	private Button mSaveButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplay);
		UIUtils.setTitle(this);
		setUiVariables();
		mLogInHelper = new LogInHelper(this, this);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mGameId = intent.getExtras().getInt(KEY_GAME_ID);
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
			loadCurrentDate();
			if (mGameId == -1) {
				Log.w(TAG, "Didn't get a game ID");
				finish();
			}

			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				mGameId = intent.getExtras().getInt(KEY_GAME_ID);
				quickLogPlay();
				finish();
			} else if (!Intent.ACTION_EDIT.equals(intent.getAction())) {
				Log.w(TAG, "Received bad intent action: " + intent.getAction());
				finish();
			}
		} else {
			mGameId = savedInstanceState.getInt(KEY_GAME_ID);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mYear = savedInstanceState.getInt(yearKey);
			mMonth = savedInstanceState.getInt(monthKey);
			mDay = savedInstanceState.getInt(dayKey);
			mQuantityView.setText("" + savedInstanceState.getInt(quantityKey));
			mLengthView.setText("" + savedInstanceState.getInt(lengthKey));
			mLocationView.setText(savedInstanceState.getString(locationKey));
			mIncompleteView.setChecked(savedInstanceState.getBoolean(incompleteKey));
			mNoWinStatsView.setChecked(savedInstanceState.getBoolean(noWinStatsKey));
			mCommentsView.setText(savedInstanceState.getString(commentsKey));
		}
		((TextView) findViewById(R.id.game_name)).setText(mGameName);
		setDateButtonText();
		if (BggApplication.getInstance().getImageLoad() && !TextUtils.isEmpty(mThumbnailUrl)) {
			mThumbnail = (BezelImageView) findViewById(R.id.game_thumbnail);
			new ThumbnailTask().execute(mThumbnailUrl);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mLogInHelper.logIn();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_GAME_ID, mGameId);
		outState.putString(KEY_GAME_NAME, mGameName);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putInt(yearKey, mYear);
		outState.putInt(monthKey, mMonth);
		outState.putInt(dayKey, mDay);
		outState.putInt(quantityKey, getQuantity());
		outState.putInt(lengthKey, getLength());
		outState.putString(locationKey, getLocation());
		outState.putBoolean(incompleteKey, getIncomplete());
		outState.putBoolean(noWinStatsKey, getNoWinStats());
		outState.putString(commentsKey, getComments());
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DATE_DIALOG_ID:
				return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
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

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.save:
				logPlay(mGameId);
				return true;
			case R.id.cancel:
				finish();
				return true;
		}
		return false;
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

	public void onSaveClick(View v) {
		logPlay(mGameId);
	}

	public void onCancelClick(View v) {
		finish();
	}

	private void setUiVariables() {
		mDateButton = (Button) findViewById(R.id.logDateButton);
		mQuantityView = (EditText) findViewById(R.id.logQuantity);
		mLengthView = (EditText) findViewById(R.id.logLength);
		mLocationView = (EditText) findViewById(R.id.logLocation);
		mIncompleteView = (CheckBox) findViewById(R.id.logIncomplete);
		mNoWinStatsView = (CheckBox) findViewById(R.id.logNoWinStats);
		mCommentsView = (EditText) findViewById(R.id.logComments);
		mSaveButton = (Button) findViewById(R.id.logPlaySaveButton);
	}

	private void quickLogPlay() {
		if (mLogInHelper.checkCookies()) {
			logPlay(mGameId);
		} else {
			Toast.makeText(this, R.string.logInError, Toast.LENGTH_LONG).show();
		}
	}

	class LogPlayTask extends AsyncTask<List<NameValuePair>, Void, String> {
		@Override
		protected String doInBackground(List<NameValuePair>... params) {

			UrlEncodedFormEntity entity;
			try {
				entity = new UrlEncodedFormEntity(params[0], HTTP.ASCII);
			} catch (UnsupportedEncodingException e) {
				return e.toString();
			}
			final DefaultHttpClient client = new DefaultHttpClient();
			client.setCookieStore(mLogInHelper.getCookieStore());

			final HttpPost post = new HttpPost(BggApplication.siteUrl + "geekplay.php");
			post.setEntity(entity);

			String message = null;
			HttpResponse response = null;
			try {
				response = client.execute(post);
				if (response != null) {
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						message = HttpUtils.parseResponse(response);
					} else {
						message = getResources().getString(R.string.logInError) + " : "
								+ getResources().getString(R.string.logInErrorSuffixBadResponse) + " "
								+ response.toString() + ".";
					}
				} else {
					message = getResources().getString(R.string.logInError) + " : "
							+ getResources().getString(R.string.logInErrorSuffixNoResponse);
				}
			} catch (ClientProtocolException e) {
				return e.toString();
			} catch (IOException e) {
				return e.toString();
			}
			client.getConnectionManager().shutdown();
			return message;
		}

		@Override
		protected void onPreExecute() {
			showDialog(LOGGING_DIALOG_ID);
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d(TAG, "play result: " + result);
			removeDialog(LOGGING_DIALOG_ID);
			if (TextUtils.isEmpty(result)) {
				return;
			}
			if (result.startsWith("Plays: <a") || result.startsWith("{\"html\":\"Plays:")) {
				Log.d(TAG, result);
				int start = result.indexOf(">");
				int end = result.indexOf("<", start);
				int playCount = StringUtils.parseInt(result.substring(start + 1, end), 1);

				String countDescription = "";
				int quantity = getQuantity();
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

				Toast.makeText(getBaseContext(),
						String.format(getResources().getString(R.string.logPlaySuccess), countDescription, mGameName),
						Toast.LENGTH_LONG).show();
				finish();
			} else {
				Log.w(TAG, result);
				Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void logPlay(int gameId) {

		String date = String.format("%04d", mYear) + "-" + String.format("%02d", mMonth + 1) + "-"
				+ String.format("%02d", mDay);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ajax", "1"));
		nvps.add(new BasicNameValuePair("action", "save"));
		nvps.add(new BasicNameValuePair("version", "2"));
		nvps.add(new BasicNameValuePair("objecttype", "thing"));
		nvps.add(new BasicNameValuePair("objectid", "" + gameId));
		nvps.add(new BasicNameValuePair("playdate", date));
		// TODO: ask Aldie what this is
		// String today = android.text.format.DateFormat.format("yyyy-MM-dd",
		// new Date()).toString();
		nvps.add(new BasicNameValuePair("dateinput", date));
		nvps.add(new BasicNameValuePair("length", "" + getLength()));
		nvps.add(new BasicNameValuePair("location", getLocation()));
		nvps.add(new BasicNameValuePair("quantity", "" + getQuantity()));
		nvps.add(new BasicNameValuePair("incomplete", getIncomplete() ? "1" : "0"));
		nvps.add(new BasicNameValuePair("nowinstats", getNoWinStats() ? "1" : "0"));
		nvps.add(new BasicNameValuePair("comments", getComments()));
		Log.d(TAG, nvps.toString());

		LogPlayTask task = new LogPlayTask();
		task.execute(nvps);
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			setDateButtonText();
		}
	};

	private void setDateButtonText() {
		mDateButton.setText(df.format(new Date(mYear - 1900, mMonth, mDay)));
	}

	private int getQuantity() {
		return StringUtils.parseInt(mQuantityView.getText().toString(), 1);
	}

	private int getLength() {
		return StringUtils.parseInt(mLengthView.getText().toString(), 0);
	}

	private String getLocation() {
		return mLocationView.getText().toString();
	}

	private boolean getIncomplete() {
		return mIncompleteView.isChecked();
	}

	private boolean getNoWinStats() {
		return mNoWinStatsView.isChecked();
	}

	private String getComments() {
		return mCommentsView.getText().toString();
	}

	private void loadCurrentDate() {
		final Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
	}

	private void enableSave() {
		mSaveButton.setEnabled(true);
	}

	private class ThumbnailTask extends AsyncTask<String, Void, Drawable> {

		@Override
		protected void onPreExecute() {
			findViewById(R.id.thumbnail_progress).setVisibility(View.VISIBLE);
		}

		@Override
		protected Drawable doInBackground(String... params) {
			return ImageCache.getImage(LogPlayActivity.this, params[0]);
		}

		@Override
		protected void onPostExecute(Drawable result) {
			findViewById(R.id.thumbnail_progress).setVisibility(View.GONE);
			mThumbnail.setVisibility(View.VISIBLE);
			if (result != null) {
				mThumbnail.setImageDrawable(result);
			} else {
				mThumbnail.setImageResource(R.drawable.noimage);
			}
		}
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
}
