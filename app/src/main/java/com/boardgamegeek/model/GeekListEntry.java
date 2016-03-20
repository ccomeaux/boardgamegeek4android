package com.boardgamegeek.model;

import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract;

public class GeekListEntry {
	private String href;
	private int numpositive;
	private String username;
	private int numitems;
	private String title;
	private String postdate; // always null
	private String lastreplydate; // always null
	private String pagination; // not useful

	public String getTitle() {
		return title;
	}

	public int getId() {
		if (TextUtils.isEmpty(href)){
			return BggContract.INVALID_ID;
		}
		int start = href.indexOf("/geeklist/");
		return Integer.valueOf(href.substring(start + 10, href.lastIndexOf("/")));
	}

	public String getAuthor() {
		return username;
	}

	public int getNumberOfItems() {
		return numitems;
	}

	public int getNumberOfThumbs() {
		return numpositive;
	}

	@Override
	public String toString() {
		return getId() + ": " + title;
	}
}
