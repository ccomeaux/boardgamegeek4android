package com.boardgamegeek.io

import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.Context
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.util.HttpUtils
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

class AuthInterceptor(private val context: Context?) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (context != null) {
            val accountManager = AccountManager.get(context)
            val account = Authenticator.getAccount(accountManager)
            if (account != null) {
                try {
                    val password = accountManager.blockingGetAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, true)
                    if (account.name.isNotBlank() && password.isNotBlank()) {
                        val username = HttpUtils.encode(account.name)
                        val cookieValue = "bggusername=$username; bggpassword=$password"
                        val request = originalRequest.newBuilder().addHeader("Cookie", cookieValue).build()
                        return chain.proceed(request)
                    }
                } catch (e: OperationCanceledException) {
                    Timber.w(e)
                } catch (e: AuthenticatorException) {
                    Timber.w(e)
                } catch (e: SecurityException) {
                    Timber.w(e)
                }
            }
        }
        return chain.proceed(originalRequest)
    }
}
