package com.boardgamegeek.provider;

import android.app.SearchManager;

public class SearchSuggestTextProvider extends SearchSuggestProvider {
	@Override
	protected String getPath() {
		return SearchManager.SUGGEST_URI_PATH_QUERY + "/*";
	}
}
