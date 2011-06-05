package com.boardgamegeek.util;

import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class Cookies {
	private static final int COOKIE_COUNT = 10;
	private static final String KEY_EXPIRYDATE = "expirydate";
	private static final String KEY_DOMAIN = "domain";
	private static final String KEY_PATH = "path";
	private static final String KEY_NAME = "name";
	private static final String KEY_VALUE = "value";
	private static final String KEY_PREFIX = "cookie";

	private Context mContext;
	private CookieStore mCookieStore;

	public Cookies(Context context) {
		mContext = context;
	}

	public boolean clearCookies() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = preferences.edit();
		for (int i = 0; i < COOKIE_COUNT; i++) {
			removeCookie(editor, i);
		}
		return editor.commit();
	}

	public boolean saveCookies() {
		if (mCookieStore == null) {
			return false;
		}

		List<Cookie> cookies = mCookieStore.getCookies();
		if (cookies == null || cookies.size() == 0) {
			return false;
		}

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = preferences.edit();
		for (int i = 0; i < COOKIE_COUNT; i++) {
			if (i < cookies.size()) {
				Cookie cookie = cookies.get(i);
				editor.putString(KEY_PREFIX + i + KEY_NAME, cookie.getName());
				editor.putString(KEY_PREFIX + i + KEY_VALUE, cookie.getValue());
				editor.putString(KEY_PREFIX + i + KEY_PATH, cookie.getPath());
				editor.putString(KEY_PREFIX + i + KEY_DOMAIN, cookie.getDomain());
				Date expiryDate = cookie.getExpiryDate();
				if (expiryDate != null) {
					editor.putLong(KEY_PREFIX + i + KEY_EXPIRYDATE, expiryDate.getTime());
				}
			} else {
				removeCookie(editor, i);
			}
		}
		return editor.commit();
	}

	public CookieStore loadCookies() {
		mCookieStore = null;
		BasicCookieStore cookieStore = new BasicCookieStore();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		for (int i = 0; i < COOKIE_COUNT; i++) {
			String name = preferences.getString(KEY_PREFIX + i + KEY_NAME, "");
			String value = preferences.getString(KEY_PREFIX + i + KEY_VALUE, "");
			if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
				BasicClientCookie cookie = new BasicClientCookie(name, value);
				cookie.setPath(preferences.getString(KEY_PREFIX + i + KEY_PATH, ""));
				cookie.setDomain(preferences.getString(KEY_PREFIX + i + KEY_DOMAIN, ""));
				cookie.setExpiryDate(new Date(preferences.getLong(KEY_PREFIX + i + KEY_EXPIRYDATE, 0)));
				cookieStore.addCookie(cookie);
			} else {
				break;
			}
		}
		if (cookieStore.getCookies() != null && cookieStore.getCookies().size() > 0) {
			mCookieStore = cookieStore;
		}
		return mCookieStore;
	}

	public CookieStore getCookieStore() {
		return mCookieStore;
	}

	public void setCookieStore(CookieStore cookieStore) {
		mCookieStore = cookieStore;
	}

	private void removeCookie(Editor editor, int i) {
		editor.remove(KEY_PREFIX + i + KEY_VALUE);
		editor.remove(KEY_PREFIX + i + KEY_NAME);
		editor.remove(KEY_PREFIX + i + KEY_PATH);
		editor.remove(KEY_PREFIX + i + KEY_DOMAIN);
		editor.remove(KEY_PREFIX + i + KEY_EXPIRYDATE);
	}
}
