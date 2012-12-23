package com.boardgamegeek.util;

import java.util.List;

import android.text.TextUtils;

public class GameUrlBuilder extends UrlBuilder {
	private final List<String> mGameIds;
	private boolean mStats;

	public GameUrlBuilder(List<String> ids) {
		mGameIds = ids;
	}

	public GameUrlBuilder stats() {
		mStats = true;
		return this;
	}

	public String build() {
		if (mGameIds == null || mGameIds.size() == 0) {
			return null;
		}
		String url = BASE_URL + "boardgame/" + TextUtils.join(",", mGameIds);
		return url + (mStats ? "?stats=1" : "");
	}
}
