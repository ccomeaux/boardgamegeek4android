package com.boardgamegeek.repository

import android.content.Context
import androidx.core.os.bundleOf
import com.boardgamegeek.auth.BggCookieJar
import com.boardgamegeek.entities.AuthEntity
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
    val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun authenticate(username: String, password: String, method: String): AuthEntity? {
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
    private fun tryAuthenticate(username: String, password: String, method: String): AuthEntity? {
        val cookieJar = BggCookieJar()
        val client = httpClient.newBuilder().cookieJar(cookieJar).build()
        val response = client.newCall(buildRequest(username, password)).execute()
        return if (response.isSuccessful && !cookieJar.authToken.isNullOrBlank()) {
            log(method)
            AuthEntity(
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
}
