package com.boardgamegeek.ui

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.boardgamegeek.R
import com.boardgamegeek.auth.Authenticator
import com.boardgamegeek.databinding.ActivityLoginBinding
import com.boardgamegeek.extensions.*
import com.boardgamegeek.entities.AuthToken
import com.boardgamegeek.ui.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Activity which displays a login screen to the user, offering registration as well.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel by viewModels<LoginViewModel>()
    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null

    private var username: String? = null
    private var password: String? = null

    private lateinit var accountManager: AccountManager
    private var isRequestingNewAccount = false
    private var isAuthenticating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            if (isAuthenticating) viewModel.cancel() else finish()
        }

        accountAuthenticatorResponse = intent.getParcelableCompat(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        accountManager = AccountManager.get(this)
        username = intent.getStringExtra(KEY_USERNAME)
        isRequestingNewAccount = username == null

        binding.usernameView.setText(username)
        binding.usernameView.isEnabled = isRequestingNewAccount
        if (isRequestingNewAccount) {
            binding.usernameView.requestFocus()
        } else {
            binding.passwordView.requestFocus()
        }
        binding.passwordView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == R.integer.login_action_id || actionId == EditorInfo.IME_NULL) {
                attemptLogin()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.signInButton.setOnClickListener {
            attemptLogin()
        }

        viewModel.isAuthenticating.observe(this) {
            isAuthenticating = it ?: false
            showProgress(isAuthenticating)
        }

        viewModel.authenticationResult.observe(this) {
            if (it == null) {
                binding.passwordContainer.error = getString(R.string.error_incorrect_password)
                binding.passwordView.requestFocus()
            } else {
                createAccount(it)
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form errors (invalid email,
     * missing fields, etc.), the errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (isAuthenticating) {
            return
        }

        // Reset errors.
        binding.usernameContainer.error = null
        binding.passwordContainer.error = null

        // Store values at the time of the login attempt.
        if (isRequestingNewAccount) {
            username = binding.usernameView.text.toString().trim()
        }
        password = binding.passwordView.text.toString()
        val focusView = when {
            username.isNullOrBlank() -> {
                binding.usernameContainer.error = getString(R.string.error_field_required)
                binding.usernameView
            }
            password.isNullOrEmpty() -> {
                binding.passwordContainer.error = getString(R.string.error_field_required)
                binding.passwordView
            }
            else -> null
        }
        if (focusView != null) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to perform the user login attempt.
            binding.loginStatusMessageView.setText(R.string.login_progress_signing_in)
            showProgress(true)
            viewModel.login(username, password)
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        binding.loginStatusView.fade(show)
        binding.loginFormView.fade(!show)
    }

    private fun createAccount(authToken: AuthToken) {
        Timber.i("Creating account")
        val account = Account(username, Authenticator.ACCOUNT_TYPE)
        try {
            accountManager.setAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, authToken.token)
        } catch (e: SecurityException) {
            AlertDialog.Builder(this)
                .setTitle(R.string.title_error)
                .setMessage(R.string.error_account_set_auth_token_security_exception)
                .show()
            return
        }
        val userData = bundleOf(Authenticator.KEY_AUTH_TOKEN_EXPIRY to authToken.expiry.toString())
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
                            binding.passwordContainer.error = getString(R.string.error_account_list_zero)
                            return
                        }
                        accounts.size != 1 -> {
                            Timber.w("multiple accounts!")
                            binding.passwordContainer.error = getString(R.string.error_account_list_multiple, Authenticator.ACCOUNT_TYPE)
                            return
                        }
                        else -> {
                            val existingAccount = accounts[0]
                            when {
                                existingAccount == null -> {
                                    binding.passwordContainer.error = getString(R.string.error_account_list_zero)
                                    return
                                }
                                existingAccount.name != account.name -> {
                                    binding.passwordContainer.error =
                                        getString(R.string.error_account_name_mismatch, existingAccount.name, account.name)
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
                binding.passwordContainer.error = e.localizedMessage
                return
            }
        } else {
            accountManager.setPassword(account, password)
        }
        val extras = bundleOf(
            AccountManager.KEY_ACCOUNT_NAME to username,
            AccountManager.KEY_ACCOUNT_TYPE to Authenticator.ACCOUNT_TYPE
        )
        setResult(RESULT_OK, Intent().putExtras(extras))
        accountAuthenticatorResponse?.let {
            // send the result bundle back if set, otherwise send an error.
            it.onResult(extras)
            accountAuthenticatorResponse = null
        }
        preferences()[AccountPreferences.KEY_USERNAME] = username

        finish()
    }

    companion object {
        private const val KEY_USERNAME = "USERNAME"

        fun createIntentBundle(context: Context, response: AccountAuthenticatorResponse?, accountName: String?): Bundle {
            val intent = context.intentFor<LoginActivity>(
                KEY_USERNAME to accountName,
                AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE to response
            )
            return bundleOf(AccountManager.KEY_INTENT to intent)
        }
    }
}
