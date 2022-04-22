package com.boardgamegeek.auth

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import timber.log.Timber
import java.util.*

class BggCookieJar : CookieJar {
    var authToken: String? = null
        private set
    var authTokenExpiry: Long = 0
        private set

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        Timber.w("%s\n%s", url, cookies)
        cookies.find {
            "bggpassword".equals(it.name, ignoreCase = true) && !"deleted".equals(it.value, ignoreCase = true)
        }?.let { cookie ->
            authToken = cookie.value
            authTokenExpiry = cookie.expiresAt
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return emptyList()
    }

    val isValid: Boolean
        get() = !authToken.isNullOrBlank()

    override fun toString(): String {
        return "token: $authToken (${Date(authTokenExpiry)})"
    }

    companion object {
        fun generateMock(): BggCookieJar {
            return BggCookieJar().apply {
                authToken = "password"
                authTokenExpiry = Long.MAX_VALUE
            }
        }
    }
}
