package com.boardgamegeek.auth

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.*

class BggCookieJar : CookieJar {
    var authToken: String? = null
        private set
    var authTokenExpiry: Long = 0
        private set

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.find {
            "bggpassword".equals(it.name, ignoreCase = true) && !"deleted".equals(it.value, ignoreCase = true)
        }?.let { cookie ->
            authToken = cookie.value
            authTokenExpiry = cookie.expiresAt
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = emptyList()

    override fun toString(): String = "token: $authToken (${Date(authTokenExpiry)})"
}
