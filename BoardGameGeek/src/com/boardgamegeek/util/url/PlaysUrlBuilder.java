package com.boardgamegeek.util.url;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.text.TextUtils;

public class PlaysUrlBuilder extends UrlBuilder {
	// http://boardgamegeek.com/xmlapi2/plays?username=ccomeaux&mindate=2011-11-30&maxdate=2011-12-03
	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private final String mUsername;
	private int mGameId;
	private String mMinDate;
	private String mMaxDate;
	private int mPage = 0;

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

	public PlaysUrlBuilder date(long date) {
		mMinDate = FORMAT.format(new Date(date));
		mMaxDate = FORMAT.format(new Date(date));
		return this;
	}

	public PlaysUrlBuilder minDate(String date) {
		mMinDate = date;
		return this;
	}

	public PlaysUrlBuilder minDate(long date) {
		mMinDate = FORMAT.format(new Date(date));
		return this;
	}

	public PlaysUrlBuilder maxDate(String date) {
		mMaxDate = date;
		return this;
	}

	public PlaysUrlBuilder maxDate(long date) {
		mMaxDate = FORMAT.format(new Date(date));
		return this;
	}

	public PlaysUrlBuilder page(int page) {
		mPage = page;
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
		if (mPage > 0) {
			url += "&page=" + mPage;
		}
		return url;
	}
}