package com.boardgamegeek.auth

import android.content.Context
import androidx.core.os.bundleOf
import com.boardgamegeek.auth.BggCookieJar.Companion.generateMock
import com.boardgamegeek.util.HttpUtils
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonObject
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.nio.charset.StandardCharsets

object NetworkAuthenticator {
    private const val MOCK_LOGIN = false

    /**
     * Authenticates to BGG with the specified username and password, returning the cookie store to use on subsequent
     * requests, or null if authentication fails.
     */
    fun authenticate(username: String, password: String, method: String, context: Context): BggCookieJar? {
        return if (MOCK_LOGIN) {
            generateMock()
        } else {
            tryAuthenticate(username, password, method, context)
        }
    }

    private fun tryAuthenticate(username: String, password: String, method: String, context: Context): BggCookieJar? {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        try {
            return performAuthenticate(username, password, method, firebaseAnalytics)
        } catch (e: IOException) {
            logAuthFailure(method, "IOException", firebaseAnalytics)
        } finally {
            Timber.w("Authentication complete")
        }
        return null
    }

    @Throws(IOException::class)
    private fun performAuthenticate(username: String, password: String, method: String, firebaseAnalytics: FirebaseAnalytics): BggCookieJar? {
        val cookieJar = BggCookieJar()
        val client = HttpUtils.getHttpClient(false).newBuilder()
            .cookieJar(cookieJar)
            .build()
        val post = buildRequest(username, password)
        val response = client.newCall(post).execute()
        if (response.isSuccessful) {
            if (cookieJar.isValid) {
                val bundle = bundleOf(
                    FirebaseAnalytics.Param.METHOD to method,
                    FirebaseAnalytics.Param.SUCCESS to "true",
                )
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
                return cookieJar
            } else {
                logAuthFailure(method, "Invalid cookie jar", firebaseAnalytics)
            }
        } else {
            logAuthFailure(method, "Response: $response", firebaseAnalytics)
        }
        return null
    }

    private fun logAuthFailure(method: String, reason: String, firebaseAnalytics: FirebaseAnalytics) {
        Timber.w("Failed %1\$s login: %2\$s", method, reason)
        val bundle = bundleOf(
            FirebaseAnalytics.Param.METHOD to method,
            FirebaseAnalytics.Param.SUCCESS to "false",
            "Reason" to reason,
        )
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    private fun buildRequest(username: String, password: String): Request {
        val body = JsonObject().apply {
            add("credentials", JsonObject().apply {
                addProperty("username", username)
                addProperty("password", password)
            })
        }
        return Request.Builder()
            .url("https://boardgamegeek.com/login/api/v1")
            .post(body.toString().toByteArray(StandardCharsets.UTF_8).toRequestBody())
            .addHeader("Content-Type", "application/json")
            .build()
    }
}
