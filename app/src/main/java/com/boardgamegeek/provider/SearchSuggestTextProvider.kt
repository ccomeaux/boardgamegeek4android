package com.boardgamegeek.provider

import android.app.SearchManager

class SearchSuggestTextProvider : SearchSuggestProvider() {
    override val path = "${SearchManager.SUGGEST_URI_PATH_QUERY}/*"
}