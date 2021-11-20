package com.boardgamegeek.auth

import android.accounts.*
import android.annotation.TargetApi
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.core.os.bundleOf
import com.boardgamegeek.extensions.AccountPreferences
import com.boardgamegeek.extensions.preferences
import com.boardgamegeek.extensions.set
import com.boardgamegeek.ui.LoginActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import java.io.IOException

class Authenticator(private val context: Context?) : AbstractAccountAuthenticator(context) {
    @Throws(NetworkErrorException::class)
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String,
        requiredFeatures: Array<String>,
        options: Bundle
    ): Bundle {
        Timber.v("Adding account: accountType=%s, authTokenType=%s", accountType, authTokenType)
        return LoginActivity.createIntentBundle(context!!, response, null)
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account, options: Bundle): Bundle? {
        Timber.v("confirmCredentials is not supported. If it was we would ask the user for their password.")
        return null
    }

    override fun editProperties(response: AccountAuthenticatorResponse, accountType: String): Bundle {
        Timber.v("editProperties is not supported.")
        throw UnsupportedOperationException()
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle): Bundle {
        Timber.v("getting auth token...")

        // If the caller requested an authToken type we don't support, then return an error
        if (authTokenType != AUTH_TOKEN_TYPE) {
            return bundleOf(AccountManager.KEY_ERROR_MESSAGE to "invalid authTokenType")
        }
        val am = AccountManager.get(context)

        // Return the cached auth token (unless expired)
        val authToken = am.peekAuthToken(account, authTokenType)
        if (!authToken.isNullOrBlank()) {
            if (!isKeyExpired(am, account, KEY_AUTH_TOKEN_EXPIRY)) {
                Timber.v(toDebugString())
                return createAuthTokenBundle(account, authToken)
            }
            am.invalidateAuthToken(authTokenType, authToken)
        }

        // Ensure the password is valid and not expired, then return the stored AuthToken
        val password = am.getPassword(account)
        if (!password.isNullOrBlank()) {
            val cookieJar = NetworkAuthenticator.authenticate(account.name, password, "Renewal", context!!)
            if (cookieJar != null) {
                am.setAuthToken(account, authTokenType, cookieJar.authToken)
                am.setUserData(account, KEY_AUTH_TOKEN_EXPIRY, cookieJar.authTokenExpiry.toString())
                Timber.v(toDebugString())
                return createAuthTokenBundle(account, cookieJar.authToken)
            }
        }

        // If we get here, then we couldn't access the user's password - so we need to re-prompt them for their
        // credentials. We do that by creating an intent to display our AuthenticatorActivity panel.
        Timber.i("Expired credentials...")
        return LoginActivity.createIntentBundle(context!!, response, account.name)
    }

    override fun getAuthTokenLabel(authTokenType: String): String? {
        Timber.v("getAuthTokenLabel - we don't support multiple auth tokens")
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account, features: Array<String>): Bundle {
        Timber.v("hasFeatures - we don't support any features")
        return bundleOf(AccountManager.KEY_BOOLEAN_RESULT to false)
    }

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle): Bundle? {
        Timber.v("updateCredentials is not supported. If it was we would ask the user for their password.")
        return null
    }

    private fun createAuthTokenBundle(account: Account, authToken: String?): Bundle {
        return bundleOf(
            AccountManager.KEY_ACCOUNT_NAME to account.name,
            AccountManager.KEY_ACCOUNT_TYPE to ACCOUNT_TYPE,
            AccountManager.KEY_AUTHTOKEN to authToken,
        )
    }

    private fun isKeyExpired(am: AccountManager, account: Account, key: String): Boolean {
        val expiration = am.getUserData(account, key).toLongOrNull()
        return expiration == null || expiration < System.currentTimeMillis()
    }

    private fun toDebugString(): String {
        if (context == null) return ""
        val accountManager = AccountManager.get(context) ?: return ""
        val account = getAccount(accountManager) ?: return ""
        return """
            ACCOUNT
            Name:       ${account.name}
            Type:       ${account.type}
            Password:   ${accountManager.getPassword(account)}
            """.trimIndent()
    }

    companion object {
        const val ACCOUNT_TYPE = "com.boardgamegeek"
        const val AUTH_TOKEN_TYPE = "com.boardgamegeek"
        const val KEY_AUTH_TOKEN_EXPIRY = "AUTHTOKEN_EXPIRY"
        private const val INVALID_USER_ID = "0"
        private const val KEY_USER_ID = "com.boardgamegeek.USER_ID"

        /**
         * Gets the account associated with BoardGameGeek. Returns null if there is a problem getting the account.
         */
        fun getAccount(context: Context?): Account? {
            return if (context != null) {
                getAccount(AccountManager.get(context))
            } else null
        }

        /**
         * Gets the account associated with BoardGameGeek. Returns null if there is a problem getting the account.
         */
        fun getAccount(accountManager: AccountManager): Account? {
            val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
            if (accounts.isEmpty()) {
                // likely the user has never signed in
                Timber.v("no account!")
                return null
            } else if (accounts.size > 1) {
                Timber.w("multiple accounts!")
                return null
            }
            return accounts[0]
        }

        /**
         * Get the BGG user ID of the authenticated user.
         */
        fun getUserId(context: Context): String {
            val accountManager = AccountManager.get(context)
            val account = getAccount(accountManager) ?: return INVALID_USER_ID
            return accountManager.getUserData(account, KEY_USER_ID) ?: return INVALID_USER_ID
        }

        fun putUserId(context: Context, value: Int) {
            val accountManager = AccountManager.get(context)
            getAccount(accountManager)?.let {
                accountManager.setUserData(it, KEY_USER_ID, value.toString())
            }
        }

        /**
         * Determines if the user is signed in.
         */
        fun isSignedIn(context: Context?) = getAccount(context) != null

        fun clearPassword(context: Context?) {
            val accountManager = AccountManager.get(context)
            getAccount(accountManager)?.let { account ->
                val authToken = accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE)
                if (authToken != null) {
                    accountManager.invalidateAuthToken(AUTH_TOKEN_TYPE, authToken)
                } else {
                    accountManager.clearPassword(account)
                }
            }
        }

        fun isOldAuth(context: Context?): Boolean {
            val accountManager = AccountManager.get(context)
            val account = getAccount(accountManager)
            val data = accountManager?.getUserData(account, "PASSWORD_EXPIRY")
            return data != null
        }

        fun signOut(context: Context) {
            val accountManager = AccountManager.get(context)
            getAccount(accountManager)?.let { account ->
                removeAccountCompat(context, account, true)
                accountManager.setUserData(account, KEY_USER_ID, INVALID_USER_ID)
            }
            FirebaseCrashlytics.getInstance().setUserId("")
            context.preferences()[AccountPreferences.KEY_USERNAME] = null
            context.preferences()[AccountPreferences.KEY_FULL_NAME] = null
            context.preferences()[AccountPreferences.KEY_AVATAR_URL] = null
        }

        fun removeAccounts(context: Context) {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
            for (account in accounts) {
                removeAccountCompat(context, account, false)
                accountManager.setUserData(account, KEY_USER_ID, INVALID_USER_ID)
            }
        }

        private fun removeAccountCompat(context: Context, account: Account, postEvent: Boolean) {
            if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP_MR1) {
                removeAccountWithActivity(context, account, postEvent)
            } else {
                removeAccount(context, account, postEvent)
            }
        }

        private fun removeAccount(context: Context, account: Account, postEvent: Boolean) {
            @Suppress("DEPRECATION")
            AccountManager.get(context).removeAccount(account, { future ->
                if (future.isDone) {
                    try {
                        if (postEvent && future.result)
                            context.preferences()[AccountPreferences.KEY_USERNAME] = null
                    } catch (e: OperationCanceledException) {
                        Timber.e(e, "removeAccount")
                    } catch (e: AuthenticatorException) {
                        Timber.e(e, "removeAccount")
                    } catch (e: IOException) {
                        Timber.e(e, "removeAccount")
                    }
                }
            }, null)
        }

        @TargetApi(VERSION_CODES.LOLLIPOP_MR1)
        private fun removeAccountWithActivity(context: Context, account: Account, postEvent: Boolean) {
            AccountManager.get(context).removeAccount(account, null, { future ->
                if (future.isDone) {
                    try {
                        if (postEvent && future.result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT)) {
                            context.preferences()[AccountPreferences.KEY_USERNAME] = null
                        }
                    } catch (e: OperationCanceledException) {
                        Timber.e(e, "removeAccount")
                    } catch (e: AuthenticatorException) {
                        Timber.e(e, "removeAccount")
                    } catch (e: IOException) {
                        Timber.e(e, "removeAccount")
                    }
                }
            }, null)
        }
    }
}
