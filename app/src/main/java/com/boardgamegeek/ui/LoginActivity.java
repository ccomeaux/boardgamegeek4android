package com.boardgamegeek.ui;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.auth.BggCookieJar;
import com.boardgamegeek.auth.NetworkAuthenticator;
import com.boardgamegeek.events.SignInEvent;
import com.boardgamegeek.extensions.TaskUtils;
import com.google.android.material.textfield.TextInputLayout;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Activity which displays a login screen to the user, offering registration as well.
 */
public class LoginActivity extends AccountAuthenticatorActivity {
	private static final String KEY_USERNAME = "USERNAME";

	private String username;
	private String password;

	@BindView(R.id.username_container) TextInputLayout usernameContainer;
	@BindView(R.id.username) EditText usernameView;
	@BindView(R.id.password_container) TextInputLayout passwordContainer;
	@BindView(R.id.password) EditText passwordView;
	@BindView(R.id.login_form) View loginFormView;
	@BindView(R.id.login_status) View loginStatusView;
	@BindView(R.id.login_status_message) TextView loginStatusMessageView;

	private UserLoginTask userLoginTask = null;
	private AccountManager accountManager;
	private boolean isRequestingNewAccount;

	@NonNull
	public static Bundle createIntentBundle(Context context, AccountAuthenticatorResponse response, String accountName) {
		final Intent intent = new Intent(context, LoginActivity.class);
		if (!TextUtils.isEmpty(accountName)) {
			intent.putExtra(KEY_USERNAME, accountName);
		}
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		ButterKnife.bind(this);

		accountManager = AccountManager.get(this);
		username = getIntent().getStringExtra(KEY_USERNAME);

		isRequestingNewAccount = username == null;

		usernameView.setText(username);
		usernameView.setEnabled(isRequestingNewAccount);
		if (!isRequestingNewAccount) {
			passwordView.requestFocus();
		}

		passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
				if (actionId == R.integer.login_action_id || actionId == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onBackPressed() {
		if (userLoginTask != null) {
			userLoginTask.cancel(true);
		} else {
			super.onBackPressed();
		}
	}

	@OnClick(R.id.sign_in_button)
	public void onSignInClick() {
		attemptLogin();
	}

	/**
	 * Attempts to sign in or register the account specified by the login form. If there are form errors (invalid email,
	 * missing fields, etc.), the errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (userLoginTask != null) {
			return;
		}

		// Reset errors.
		usernameContainer.setError(null);
		passwordContainer.setError(null);

		// Store values at the time of the login attempt.
		if (isRequestingNewAccount) {
			username = usernameView.getText().toString().trim();
		}
		password = passwordView.getText().toString();

		View focusView = null;

		if (TextUtils.isEmpty(password)) {
			passwordContainer.setError(getString(R.string.error_field_required));
			focusView = passwordView;
		}

		if (TextUtils.isEmpty(username)) {
			usernameContainer.setError(getString(R.string.error_field_required));
			focusView = usernameView;
		}

		if (focusView != null) {
			// There was an error; don't attempt login and focus the first form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to perform the user login attempt.
			loginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			userLoginTask = new UserLoginTask();
			TaskUtils.executeAsyncTask(userLoginTask);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	private void showProgress(final boolean show) {
		int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

		loginStatusView.setVisibility(View.VISIBLE);
		loginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					loginStatusView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
				}
			});

		loginFormView.setVisibility(View.VISIBLE);
		loginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					loginFormView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
				}
			});
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, BggCookieJar> {
		@Override
		protected BggCookieJar doInBackground(Void... params) {
			return NetworkAuthenticator.authenticate(username, password, "Dialog");
		}

		@Override
		protected void onPostExecute(BggCookieJar bggCookieJar) {
			userLoginTask = null;
			showProgress(false);

			if (bggCookieJar != null) {
				createAccount(bggCookieJar);
			} else {
				passwordContainer.setError(getString(R.string.error_incorrect_password));
				passwordView.requestFocus();
			}
		}

		@Override
		protected void onCancelled() {
			userLoginTask = null;
			showProgress(false);
		}
	}

	private void createAccount(BggCookieJar bggCookieJar) {
		Timber.i("Creating account");
		final Account account = new Account(username, Authenticator.ACCOUNT_TYPE);

		try {
			accountManager.setAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, bggCookieJar.getAuthToken());
		} catch (SecurityException e) {
			showError(R.string.error_account_set_auth_token_security_exception);
			return;
		}
		Bundle userData = new Bundle();
		userData.putString(Authenticator.KEY_AUTH_TOKEN_EXPIRY, String.valueOf(bggCookieJar.getAuthTokenExpiry()));

		if (isRequestingNewAccount) {
			try {
				boolean success = accountManager.addAccountExplicitly(account, password, userData);
				if (!success) {
					Authenticator.removeAccounts(getApplicationContext());
					success = accountManager.addAccountExplicitly(account, password, userData);
				}
				if (!success) {
					Account[] accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
					if (accounts.length == 0) {
						Timber.v("no account!");
						passwordContainer.setError(getString(R.string.error_account_list_zero));
						return;
					} else if (accounts.length != 1) {
						Timber.w("multiple accounts!");
						passwordContainer.setError(getString(R.string.error_account_list_multiple, Authenticator.ACCOUNT_TYPE));
						return;
					} else {
						Account existingAccount = accounts[0];
						if (existingAccount == null) {
							passwordContainer.setError(getString(R.string.error_account_list_zero));
							return;
						} else if (!existingAccount.name.equals(account.name)) {
							passwordContainer.setError(getString(R.string.error_account_name_mismatch, existingAccount.name, account.name));
							return;
						} else {
							accountManager.setPassword(account, password);
						}
					}
				}
			} catch (Exception e) {
				passwordContainer.setError(e.getLocalizedMessage());
				return;
			}
		} else {
			accountManager.setPassword(account, password);
		}

		EventBus.getDefault().post(new SignInEvent(username));

		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);

		finish();
	}

	private void showError(@StringRes int message) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.title_error)
			.setMessage(message)
			.show();
	}
}
