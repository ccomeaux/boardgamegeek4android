package com.boardgamegeek.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;

public class LogInHelper {
	private static final String TAG = "LogInTask";

	private Context mContext;
	private LogInListener mListener;
	private Cookies mCookies;
	private String mUsername;
	private String mPassword;

	public LogInHelper(Context context, LogInListener listner) {
		mContext = context;
		mListener = listner;
		mCookies = new Cookies(mContext);
	}

	public CookieStore getCookieStore() {
		return mCookies.getCookieStore();
	}

	public CookieStore logIn() {
		if (checkCookies()) {
			if (mListener != null) {
				mListener.onLogInSuccess();
			}
			return getCookieStore();
		}

		if (!canLogIn()) {
			if (mListener != null) {
				mListener.onNeedCredentials();
			}
			return null;
		}

		 new LogInTask().execute();
		 return null;
	}

	public boolean checkCookies() {
		if (mCookies.getCookieStore() != null) {
			return true;
		}
		if (mCookies.loadCookies() != null) {
			return true;
		}
		return false;
	}

	public boolean canLogIn() {
		mUsername = BggApplication.getInstance().getUserName();
		mPassword = BggApplication.getInstance().getPassword();
		if (TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
			return false;
		}
		return true;
	}

	class LogInTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			if (!canLogIn()) {
				return mContext.getResources().getString(R.string.setUsernamePassword);
			}

			final DefaultHttpClient client = (DefaultHttpClient) HttpUtils.createHttpClient(mContext, false);
			final HttpPost post = new HttpPost(BggApplication.siteUrl + "login");
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
				return createErrorMessage(R.string.logInErrorSuffixNoResponse);
			}

			Log.i(TAG, response.toString());
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return createErrorMessage(R.string.logInErrorSuffixBadResponse);
			}

			List<Cookie> cookies = client.getCookieStore().getCookies();
			if (cookies == null || cookies.isEmpty()) {
				return createErrorMessage(R.string.logInErrorSuffixMissingCookies);
			}

			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("bggpassword")) {
					mCookies.setCookieStore(client.getCookieStore());
					break;
				}
			}
			if (mCookies.getCookieStore() == null) {
				return createErrorMessage(R.string.logInErrorSuffixBadCookies);
			}

			mCookies.saveCookies();
			return null;
		}

		private String createErrorMessage(int resId) {
			Resources r = mContext.getResources();
			String message;
			message = r.getString(R.string.logInError) + "\n(" + r.getString(resId) + ")";
			return message;
		}

		@Override
		protected void onPostExecute(String result) {
			if (mListener != null) {
				if (!TextUtils.isEmpty(result)) {
					Log.w(TAG, result);
					mListener.onLogInError(result);
				} else {
					mListener.onLogInSuccess();
				}
			}
		}
	}

	public interface LogInListener {
		public void onLogInSuccess();

		public void onLogInError(String errorMessage);

		public void onNeedCredentials();
	}
}