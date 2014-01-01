package com.boardgamegeek.util.url;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

/**
 * Creates URL builder which will give results as:<br>
 * http://www.boardgamegeek.com/xmlapi/boardgame/13,1098&stats=1<br>
 * Invoke useNewApi() to use new API.<br>
 * http://www.boardgamegeek.com/xmlapi2/thing?id=13,1098&stats=1<br>
 * 
 * @see #useNewApi()
 */
public class GameUrlBuilder extends UrlBuilder {

	private final String mGameId;
	private int mCommentsPage;
	private int mRatingsPage;
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
		mRatingsPage = 0;
		return this;
	}

	public GameUrlBuilder ratings(int page) {
		mRatingsPage = page;
		mCommentsPage = 0;
		return this;
	}

	public GameUrlBuilder stats() {
		mStats = true;
		return this;
	}

	public GameUrlBuilder useNewApi() {
		mUseOldApi = false;
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
		} else if (mRatingsPage > 0) {
			opts.add("ratingcomments=1&page=" + mRatingsPage);
		}
		if (opts.size() > 0) {
			url += (mUseOldApi ? "?" : "&") + TextUtils.join("&", opts);
		}
		return url;
	}
}
