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
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.boardgamegeek.Preferences;
import com.boardgamegeek.R;
import com.boardgamegeek.Utility;
import com.boardgamegeek.util.UIUtils;

public class LogPlayActivity extends Activity {

	private static final int DATE_DIALOG_ID = 0;
	private static final int LOGGING_DIALOG_ID = 1;
	private static final String TAG = "LogPlayView";

	private final String gameIdKey = "GAME_ID";
	private final String gameNameKey = "GAME_NAME";
	private final String yearKey = "YEAR";
	private final String monthKey = "MONTH";
	private final String dayKey = "DAY";
	private final String quantityKey = "QUANTITY";
	private final String lengthKey = "LENGTH";
	private final String locationKey = "LOCATION";
	private final String incompleteKey = "INCOMPLETE";
	private final String noWinStatsKey = "NO_WIN_STATS";
	private final String commentsKey = "COMMENTS";

	private int mGameId;
	private String mGameName;
	private String mUsername;
	private String mPassword;
	private CookieStore mCookieStore;
	private DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
	private int mYear;
	private int mMonth;
	private int mDay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logplay);
		UIUtils.setTitle(this);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mGameId = intent.getExtras().getInt(gameIdKey);
			mGameName = intent.getExtras().getString(gameNameKey);
			loadCurrentDate();
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				mGameId = intent.getExtras().getInt(gameIdKey);
				logPlay();
				finish();
			} else if (!Intent.ACTION_EDIT.equals(intent.getAction())) {
				Log.w(TAG, "Received bad intent action: " + intent.getAction());
				finish();
			}
			if (mGameId == -1) {
				Log.w(TAG, "Didn't get a game ID");
				finish();
			}
		} else {
			mGameId = savedInstanceState.getInt(gameIdKey);
			mGameName = savedInstanceState.getString(gameNameKey);
			mYear = savedInstanceState.getInt(yearKey);
			mMonth = savedInstanceState.getInt(monthKey);
			mDay = savedInstanceState.getInt(dayKey);
			setQuantity(savedInstanceState.getInt(quantityKey));
			setLength(savedInstanceState.getInt(lengthKey));
			setLocation(savedInstanceState.getString(locationKey));
			setIncomplete(savedInstanceState.getBoolean(incompleteKey));
			setNoWinStats(savedInstanceState.getBoolean(noWinStatsKey));
			setComments(savedInstanceState.getString(commentsKey));
		}
		setTitle();
		setDateButtonText();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (getCookieStore()) {
			Button button = (Button) findViewById(R.id.logPlaySaveButton);
			button.setEnabled(true);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(gameIdKey, mGameId);
		outState.putString(gameNameKey, mGameName);
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
		menuInflater.inflate(R.menu.logplay_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.save:
			logPlay();
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
	
	public void onHomeClick(View v){
		UIUtils.goHome(this);
	}
	
	public void onSearchClick(View v){
		onSearchRequested();
	}
	
	public void onDateClick(View v) {
		showDialog(DATE_DIALOG_ID);
	}
	
	public void onSaveClick(View v) {
		logPlay();
	}
	
	public void onCancelClick(View v) {
		finish();
	}

	private void logPlay() {
		if (getCookieStore()) {
			logPlay(mGameId);
		} else {
			Toast.makeText(this, R.string.logInError, Toast.LENGTH_LONG);
		}
	}

	private boolean saveCookies(List<Cookie> cookies) {
		if (cookies == null || cookies.size() == 0) {
			return false;
		}
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = preferences.edit();
		for (int i = 0; i < 10; i++) {
			if (i < cookies.size()) {
				Cookie cookie = cookies.get(i);
				editor.putString("cookie" + i + "value", cookie.getValue());
				editor.putString("cookie" + i + "name", cookie.getName());
				editor.putString("cookie" + i + "path", cookie.getPath());
				editor.putString("cookie" + i + "domain", cookie.getDomain());
				Date expiryDate = cookie.getExpiryDate();
				if (expiryDate != null) {
					editor.putLong("cookie" + i + "expirydate", expiryDate.getTime());
				}
			} else {
				editor.remove("cookie" + i + "value");
				editor.remove("cookie" + i + "name");
				editor.remove("cookie" + i + "path");
				editor.remove("cookie" + i + "domain");
				editor.remove("cookie" + i + "expirydate");
			}
		}
		return editor.commit();
	}

	private BasicCookieStore loadCookies() {
		BasicCookieStore cookieStore = new BasicCookieStore();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		for (int i = 0; i < 10; i++) {
			String name = preferences.getString("cookie" + i + "name", "");
			String value = preferences.getString("cookie" + i + "value", "");
			if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
				BasicClientCookie cookie = new BasicClientCookie(name, value);
				cookie.setPath(preferences.getString("cookie" + i + "path", ""));
				cookie.setDomain(preferences.getString("cookie" + i + "domain", ""));
				cookie.setExpiryDate(new Date(preferences.getLong("cookie" + i + "expirydate", 0)));
				cookieStore.addCookie(cookie);
			} else {
				break;
			}
		}
		if (cookieStore.getCookies() != null && cookieStore.getCookies().size() > 0) {
			return cookieStore;
		}
		return null;
	}

	private boolean getCookieStore() {

		if (mCookieStore != null) {
			return true;
		}
		mCookieStore = loadCookies();
		if (mCookieStore != null) {
			return true;
		}

		loadPreferences();
		if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
			Toast.makeText(this, R.string.setUsernamePassword, Toast.LENGTH_LONG).show();
			startActivity(new Intent(this, Preferences.class));
			finish();
			return false;
		}

		LogInTask task = new LogInTask();
		task.execute();
		return false;
	}

	class LogInTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {

			final DefaultHttpClient client = new DefaultHttpClient();
			final HttpPost post = new HttpPost(Utility.siteUrl + "login");
			List<NameValuePair> pair = new ArrayList<NameValuePair>();
			pair.add(new BasicNameValuePair("username", mUsername));
			pair.add(new BasicNameValuePair("password", mPassword));

			UrlEncodedFormEntity entity;
			try {
				entity = new UrlEncodedFormEntity(pair, HTTP.UTF_8);
			} catch (UnsupportedEncodingException e) {
				return e.toString();
			}
			post.setEntity(entity);
			String message = null;
			HttpResponse response;
			try {
				response = client.execute(post);
			} catch (ClientProtocolException e) {
				return e.toString();
			} catch (IOException e) {
				return e.toString();
			} finally {
				client.getConnectionManager().shutdown();
			}

			if (response == null) {
				message = getResources().getString(R.string.logInError) + " : "
					+ getResources().getString(R.string.logInErrorSuffixNoResponse);
			}
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				message = getResources().getString(R.string.logInError) + " : "
					+ getResources().getString(R.string.logInErrorSuffixBadResponse) + " "
					+ response.toString() + ".";
			}
			List<Cookie> cookies = client.getCookieStore().getCookies();
			if (cookies == null || cookies.isEmpty()) {
				message = getResources().getString(R.string.logInError) + " : "
					+ getResources().getString(R.string.logInErrorSuffixMissingCookies);
			}
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("bggpassword")) {
					mCookieStore = client.getCookieStore();
					break;
				}
			}
			if (mCookieStore == null) {
				message = getResources().getString(R.string.logInError) + " : "
					+ getResources().getString(R.string.logInErrorSuffixBadCookies);
			}
			if (mCookieStore != null) {
				saveCookies(mCookieStore.getCookies());
			} else {
				message = getResources().getString(R.string.logInError);
			}

			return message;
		}

		@Override
		protected void onPostExecute(String result) {
			if (!TextUtils.isEmpty(result)) {
				Log.w(TAG, result);
				Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
			} else {
				Button button = (Button) findViewById(R.id.logPlaySaveButton);
				button.setEnabled(true);
			}
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
			if (entity == null) {
				return getResources().getString(R.string.logInErrorSuffixEntityNull);
			}

			final DefaultHttpClient client = new DefaultHttpClient();
			client.setCookieStore(mCookieStore);

			final HttpPost post = new HttpPost(Utility.siteUrl + "geekplay.php");
			post.setEntity(entity);

			String message = null;
			HttpResponse response = null;
			try {
				response = client.execute(post);
				if (response != null) {
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						message = Utility.parseResponse(response);
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
				int playCount = Utility.parseInt(result.substring(start + 1, end), 1);

				String countDescription = "";
				int quantity = getQuantity();
				switch (quantity) {
				case 1:
					countDescription = Utility.getOrdinal(playCount);
					break;
				case 2:
					countDescription = Utility.getOrdinal(playCount - 1) + " & "
						+ Utility.getOrdinal(playCount);
					break;
				default:
					countDescription = Utility.getOrdinal(playCount - quantity + 1) + " - "
						+ Utility.getOrdinal(playCount);
					break;
				}

				Toast.makeText(
					getBaseContext(),
					String.format(getResources().getString(R.string.logPlaySuccess), countDescription,
						mGameName), Toast.LENGTH_LONG).show();
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

	private void loadPreferences() {
		mUsername = BggApplication.getInstance().getUserName();
		mPassword = BggApplication.getInstance().getPassword();
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			setDateButtonText();
		}
	};

	private void setTitle() {
		((TextView)findViewById(R.id.game_name)).setText(mGameName);
	}

	private void setDateButtonText() {
		Button button = (Button) findViewById(R.id.logDateButton);
		button.setText(df.format(new Date(mYear - 1900, mMonth, mDay)));
	}

	private int getQuantity() {
		EditText view = (EditText) findViewById(R.id.logQuantity);
		return Utility.parseInt(view.getText().toString(), 1);
	}

	private void setQuantity(int quantity) {
		EditText view = (EditText) findViewById(R.id.logQuantity);
		view.setText("" + quantity);
	}

	private int getLength() {
		EditText view = (EditText) findViewById(R.id.logLength);
		return Utility.parseInt(view.getText().toString(), 0);
	}

	private void setLength(int length) {
		EditText view = (EditText) findViewById(R.id.logLength);
		view.setText("" + length);
	}

	private String getLocation() {
		EditText view = (EditText) findViewById(R.id.logLocation);
		return view.getText().toString();
	}

	private void setLocation(String location) {
		EditText view = (EditText) findViewById(R.id.logLocation);
		view.setText(location);
	}

	private boolean getIncomplete() {
		CheckBox view = (CheckBox) findViewById(R.id.logIncomplete);
		return view.isChecked();
	}

	private void setIncomplete(boolean incomplete) {
		CheckBox view = (CheckBox) findViewById(R.id.logIncomplete);
		view.setChecked(incomplete);
	}

	private boolean getNoWinStats() {
		CheckBox view = (CheckBox) findViewById(R.id.logNoWinStats);
		return view.isChecked();
	}

	private void setNoWinStats(boolean noWinStats) {
		CheckBox view = (CheckBox) findViewById(R.id.logNoWinStats);
		view.setChecked(noWinStats);
	}

	private String getComments() {
		EditText view = (EditText) findViewById(R.id.logComments);
		return view.getText().toString();
	}

	private void setComments(String comments) {
		EditText view = (EditText) findViewById(R.id.logComments);
		view.setText(comments);
	}

	private void loadCurrentDate() {
		final Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
	}
}
