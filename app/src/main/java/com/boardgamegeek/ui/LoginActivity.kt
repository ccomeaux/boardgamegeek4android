package com.boardgamegeek.ui

import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.auth.BggCookieJar
import com.boardgamegeek.auth.NetworkAuthenticator
import com.boardgamegeek.events.SignInEvent
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.extensions.fade
import kotlinx.android.synthetic.main.activity_login.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.intentFor
import timber.log.Timber

/**
 * Activity which displays a login screen to the user, offering registration as well.
 */
class LoginActivity : AccountAuthenticatorActivity() {
    private var username: String? = null
    private var password: String? = null

    private var userLoginTask: UserLoginTask? = null
    private lateinit var accountManager: AccountManager
    private var isRequestingNewAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        accountManager = AccountManager.get(this)
        username = intent.getStringExtra(KEY_USERNAME)
        isRequestingNewAccount = username == null

        usernameView.setText(username)
        usernameView.isEnabled = isRequestingNewAccount
        if (!isRequestingNewAccount) {
            passwordView.requestFocus()
        }
        passwordView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == R.integer.login_action_id || actionId == EditorInfo.IME_NULL) {
                attemptLogin()
                return@setOnEditorActionListener true
            }
            false
        }
        signInButton.setOnClickListener {
            attemptLogin()
        }
    }

    override fun onBackPressed() {
        if (userLoginTask != null) {
            userLoginTask?.cancel(true)
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form errors (invalid email,
     * missing fields, etc.), the errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (userLoginTask != null) {
            return
        }

        // Reset errors.
        usernameContainer.error = null
        passwordContainer.error = null

        // Store values at the time of the login attempt.
        if (isRequestingNewAccount) {
            username = usernameView.text.toString().trim()
        }
        password = passwordView.text.toString()
        val focusView = when {
            username.isNullOrBlank() -> {
                usernameContainer.error = getString(R.string.error_field_required)
                usernameView
            }
            password.isNullOrEmpty() -> {
                passwordContainer.error = getString(R.string.error_field_required)
                passwordView
            }
            else -> null
        }
        if (focusView != null) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to perform the user login attempt.
            loginStatusMessageView.setText(R.string.login_progress_signing_in)
            showProgress(true)
            userLoginTask = UserLoginTask()
            userLoginTask?.executeAsyncTask()
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        loginStatusView.fade(show)
        loginFormView.fade(!show)
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate the user.
     */
    inner class UserLoginTask : AsyncTask<Void?, Void?, BggCookieJar?>() {
        override fun doInBackground(vararg params: Void?): BggCookieJar? {
            return NetworkAuthenticator.authenticate(username ?: "", password
                    ?: "", "Dialog", applicationContext)
        }

        override fun onPostExecute(bggCookieJar: BggCookieJar?) {
            userLoginTask = null
            showProgress(false)
            if (bggCookieJar != null) {
                createAccount(bggCookieJar)
            } else {
                passwordContainer.error = getString(R.string.error_incorrect_password)
                passwordView.requestFocus()
            }
        }

        override fun onCancelled() {
            userLoginTask = null
            showProgress(false)
        }
    }

    private fun createAccount(bggCookieJar: BggCookieJar) {
        Timber.i("Creating account")
        val account = Account(username, Authenticator.ACCOUNT_TYPE)
        try {
            accountManager.setAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, bggCookieJar.authToken)
        } catch (e: SecurityException) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.title_error)
                    .setMessage(R.string.error_account_set_auth_token_security_exception)
                    .show()
            return
        }
        val userData = bundleOf(Authenticator.KEY_AUTH_TOKEN_EXPIRY to bggCookieJar.authTokenExpiry.toString())
        if (isRequestingNewAccount) {
            try {
                var success = accountManager.addAccountExplicitly(account, password, userData)
                if (!success) {
                    Authenticator.removeAccounts(applicationContext)
                    success = accountManager.addAccountExplicitly(account, password, userData)
                }
                if (!success) {
                    val accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE)
                    when {
                        accounts.isEmpty() -> {
                            Timber.v("no account!")
                            passwordContainer.error = getString(R.string.error_account_list_zero)
                            return
                        }
                        accounts.size != 1 -> {
                            Timber.w("multiple accounts!")
                            passwordContainer.error = getString(R.string.error_account_list_multiple, Authenticator.ACCOUNT_TYPE)
                            return
                        }
                        else -> {
                            val existingAccount = accounts[0]
                            when {
                                existingAccount == null -> {
                                    passwordContainer.error = getString(R.string.error_account_list_zero)
                                    return
                                }
                                existingAccount.name != account.name -> {
                                    passwordContainer.error = getString(R.string.error_account_name_mismatch, existingAccount.name, account.name)
                                    return
                                }
                                else -> {
                                    accountManager.setPassword(account, password)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                passwordContainer.error = e.localizedMessage
                return
            }
        } else {
            accountManager.setPassword(account, password)
        }
        EventBus.getDefault().post(SignInEvent(username!!))

        val extras = bundleOf(
                AccountManager.KEY_ACCOUNT_NAME to username,
                AccountManager.KEY_ACCOUNT_TYPE to Authenticator.ACCOUNT_TYPE
        )
        setAccountAuthenticatorResult(extras)
        setResult(Activity.RESULT_OK, Intent().putExtras(extras))
        finish()
    }

    companion object {
        private const val KEY_USERNAME = "USERNAME"

        @JvmStatic
        fun createIntentBundle(context: Context, response: AccountAuthenticatorResponse?, accountName: String?): Bundle {
            val intent = context.intentFor<LoginActivity>(
                    KEY_USERNAME to accountName,
                    AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE to response
            )
            return bundleOf(AccountManager.KEY_INTENT to intent)
        }
    }
}