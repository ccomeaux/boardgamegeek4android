package com.boardgamegeek.util;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

public class GameUrlBuilder extends UrlBuilder {
	// http://www.boardgamegeek.com/xmlapi/boardgame/13,1098&stats=1
	// http://www.boardgamegeek.com/xmlapi2/thing?id=13,1098&stats=1

	private final String mGameId;
	private int mCommentsPage;
	private boolean mStats;
	private boolean mUseOldApi = true;

	public GameUrlBuilder(int gameId) {
		mGameId = String.valueOf(gameId);
	}

	public GameUrlBuilder(String gameId) {
		mGameId = gameId;
	}

	public GameUrlBuilder(List<String> gameIds) {
		mGameId = TextUtils.join(",", gameIds);
	}

	public GameUrlBuilder comments(int page) {
		mCommentsPage = page;
		return this;
	}

	public GameUrlBuilder stats() {
		mStats = true;
		return this;
	}

	public GameUrlBuilder useOldApi() {
		mUseOldApi = true;
		return this;
	}

	public String build() {
		if (TextUtils.isEmpty(mGameId)) {
			return null;
		}
		String url = null;
		if (mUseOldApi) {
			url = BASE_URL + "boardgame/" + mGameId;
		} else {
			url = BASE_URL_2 + "thing?id=" + mGameId;
		}
		List<String> opts = new ArrayList<String>();
		if (mStats) {
			opts.add("stats=1");
		}
		if (mCommentsPage > 0) {
			opts.add("comments=1&page=" + mCommentsPage);
		}
		if (opts.size() > 0) {
			url += (mUseOldApi ? "?" : "&") + TextUtils.join("&", opts);
		}
		return url;
	}
}
