package com.boardgamegeek.provider

import android.app.SearchManager

class SearchSuggestTextProvider : SearchSuggestProvider() {
    override fun getPath() = "${SearchManager.SUGGEST_URI_PATH_QUERY}/*"
}