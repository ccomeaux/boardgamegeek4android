package com.boardgamegeek.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.text.TextUtils;

public class PlaysUrlBuilder extends UrlBuilder {
	// http://boardgamegeek.com/xmlapi2/plays?username=ccomeaux&mindate=2011-11-30&maxdate=2011-12-03
	private final String mUsername;
	private final DateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private int mGameId;
	private String mMinDate;
	private String mMaxDate;

	public PlaysUrlBuilder(String username) {
		mUsername = username;
	}

	public PlaysUrlBuilder gameId(int id) {
		mGameId = id;
		return this;
	}

	public PlaysUrlBuilder date(String date) {
		mMinDate = date;
		mMaxDate = date;
		return this;
	}

	public PlaysUrlBuilder minDate(String date) {
		mMinDate = date;
		return this;
	}

	public PlaysUrlBuilder minDate(long date) {
		mMinDate = mDateFormat.format(new Date(date));
		return this;
	}

	public PlaysUrlBuilder maxDate(String date) {
		mMaxDate = date;
		return this;
	}

	public PlaysUrlBuilder maxDate(long date) {
		mMaxDate = mDateFormat.format(new Date(date));
		return this;
	}

	public String build() {
		String url = BASE_URL_2 + "plays?username=" + encode(mUsername);
		if (mGameId > 0) {
			url += "&id=" + mGameId;
		}
		if (!TextUtils.isEmpty(mMinDate)) {
			url += "&mindate=" + mMinDate;
		}
		if (!TextUtils.isEmpty(mMaxDate)) {
			url += "&maxdate=" + mMaxDate;
		}
		return url;
	}
}
