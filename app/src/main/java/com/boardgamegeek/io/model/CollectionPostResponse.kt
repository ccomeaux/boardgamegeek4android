package com.boardgamegeek.io.model

class CollectionPostResponse(val error: String? = null) {
    fun hasAuthError(): Boolean {
        return error != null && error.contains(AUTH_ERROR_TEXT, false)
    }

    companion object {
        private const val AUTH_ERROR_TEXT = "login"
    }
}
