package com.boardgamegeek.io

import android.accounts.AccountManager
import android.content.Context
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.extensions.encodeForUrl
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class AuthInterceptor(private val context: Context?) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        context?.let {
            val accountManager = AccountManager.get(context)
            Authenticator.getAccount(accountManager)?.let { account ->
                try {
                    val password = accountManager.blockingGetAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, true)
                    if (account.name.isNotBlank() && !password.isNullOrBlank()) {
                        return chain.proceed(
                            originalRequest.newBuilder()
                                .addHeader("Cookie", "bggusername=${account.name.encodeForUrl()}; bggpassword=$password")
                                .build()
                        )
                    }
                } catch (e: Exception) {
                    Timber.w(e)
                }
            }
        }
        return chain.proceed(originalRequest)
    }
}
