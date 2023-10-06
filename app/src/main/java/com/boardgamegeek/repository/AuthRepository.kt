package com.boardgamegeek.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.os.bundleOf
import com.boardgamegeek.auth.BggCookieJar
import com.boardgamegeek.entities.AuthToken
import com.boardgamegeek.extensions.*
import com.boardgamegeek.util.RemoteConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.nio.charset.StandardCharsets

class AuthRepository(
    context: Context,
    private val httpClient: OkHttpClient,
) {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val prefs: SharedPreferences by lazy { context.preferences() }

    fun authenticate(username: String, password: String, method: String): AuthToken? {
        return try {
            tryAuthenticate(username, password, method)
        } catch (e: IOException) {
            log(method, e.message.toString())
            null
        } finally {
            Timber.d("Authentication complete")
        }
    }

    @Throws(IOException::class)
    private fun tryAuthenticate(username: String, password: String, method: String): AuthToken? {
        val cookieJar = BggCookieJar()
        val client = httpClient.newBuilder().cookieJar(cookieJar).build()
        val response = client.newCall(buildRequest(username, password)).execute()
        return if (response.isSuccessful && !cookieJar.authToken.isNullOrBlank()) {
            log(method)
            AuthToken(
                cookieJar.authToken,
                cookieJar.authTokenExpiry,
            )
        } else {
            log(method, "Response: $response")
            null
        }
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

    private fun log(method: String, error: String = "") {
        if (error.isBlank())
            Timber.w("Successful $method login")
        else
            Timber.w("Failed $method login: $error")
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.LOGIN, bundleOf(
                FirebaseAnalytics.Param.METHOD to method,
                FirebaseAnalytics.Param.SUCCESS to error.isNotBlank().toString(),
                "Error" to error,
            )
        )
    }

    /***
     * Determines if the user has accepted BGG's new privacy statement.
     */
    fun hasPrivacyError(url: String): Boolean {
        val weeksToCompare = RemoteConfig.getInt(RemoteConfig.KEY_PRIVACY_CHECK_WEEKS)
        val weeks = prefs.getLastPrivacyCheckTimestamp().howManyWeeksOld()
        if (weeks < weeksToCompare) {
            Timber.i("We checked the privacy statement less than %,d weeks ago; skipping", weeksToCompare)
            return false
        }
        val request: Request = Request.Builder().url(url).build()
        return try {
            val response = httpClient.newCall(request).execute()
            val content = response.body?.string()?.trim().orEmpty()
            if (content.contains("Please update your privacy and marketing preferences")) {
                true
            } else {
                prefs.setLastPrivacyCheckTimestamp()
                false
            }
        } catch (e: IOException) {
            Timber.w(e)
            true
        }
    }
}
