package com.boardgamegeek.ui;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog.Builder;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.auth.BggCookieJar;
import com.boardgamegeek.auth.NetworkAuthenticator;
import com.boardgamegeek.util.ActivityUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Activity which displays a login screen to the user, offering registration as well.
 */
public class LoginActivity extends AccountAuthenticatorActivity {
	private String username;
	private String password;

	@SuppressWarnings("unused") @Bind(R.id.username) EditText usernameView;
	@SuppressWarnings("unused") @Bind(R.id.password) EditText passwordView;
	@SuppressWarnings("unused") @Bind(R.id.login_form) View loginFormView;
	@SuppressWarnings("unused") @Bind(R.id.login_status) View loginStatusView;
	@SuppressWarnings("unused") @Bind(R.id.login_status_message) TextView loginStatusMessageView;

	private UserLoginTask userLoginTask = null;
	private AccountManager accountManager;
	private boolean isRequestingNewAccount;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		ButterKnife.bind(this);

		accountManager = AccountManager.get(this);
		username = getIntent().getStringExtra(ActivityUtils.KEY_USER);

		isRequestingNewAccount = username == null;

		usernameView.setText(username);
		usernameView.setEnabled(isRequestingNewAccount);
		if (!isRequestingNewAccount) {
			passwordView.requestFocus();
		}

		passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
				if (actionId == R.id.login || actionId == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});
	}

	@DebugLog
	@Override
	public void onBackPressed() {
		if (userLoginTask != null) {
			userLoginTask.cancel(true);
		} else {
			super.onBackPressed();
		}
	}

	@DebugLog
	@SuppressWarnings({ "UnusedParameters", "unused" })
	@OnCheckedChanged(R.id.show_password)
	public void onShowPasswordCheckChanged(CompoundButton buttonView, boolean isChecked) {
		int selectionStart = passwordView.getSelectionStart();
		int selectionEnd = passwordView.getSelectionEnd();
		passwordView.setInputType(isChecked ?
			(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) :
			(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
		if (selectionStart >= 0 && selectionEnd >= 0) {
			passwordView.setSelection(selectionStart, selectionEnd);
		}
	}

	@DebugLog
	@SuppressWarnings({ "UnusedParameters", "unused" })
	@OnClick(R.id.sign_in_button)
	public void onSignInClick(View view) {
		attemptLogin();
	}

	/**
	 * Attempts to sign in or register the account specified by the login form. If there are form errors (invalid email,
	 * missing fields, etc.), the errors are presented and no actual login attempt is made.
	 */
	@DebugLog
	public void attemptLogin() {
		if (userLoginTask != null) {
			return;
		}

		// Reset errors.
		usernameView.setError(null);
		passwordView.setError(null);

		// Store values at the time of the login attempt.
		if (isRequestingNewAccount) {
			username = usernameView.getText().toString();
		}
		password = passwordView.getText().toString();

		View focusView = null;

		if (TextUtils.isEmpty(password)) {
			passwordView.setError(getString(R.string.error_field_required));
			focusView = passwordView;
		}

		if (TextUtils.isEmpty(username)) {
			usernameView.setError(getString(R.string.error_field_required));
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
			userLoginTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@DebugLog
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
			return NetworkAuthenticator.authenticate(username, password);
		}

		@Override
		protected void onPostExecute(BggCookieJar bggCookieJar) {
			userLoginTask = null;
			showProgress(false);

			if (bggCookieJar != null) {
				createAccount(bggCookieJar);
			} else {
				passwordView.setError(getString(R.string.error_incorrect_password));
				passwordView.requestFocus();
			}
		}

		@Override
		protected void onCancelled() {
			userLoginTask = null;
			showProgress(false);
		}
	}

	@DebugLog
	private void createAccount(BggCookieJar bggCookieJar) {
		Timber.i("Creating account");
		final Account account = new Account(username, Authenticator.ACCOUNT_TYPE);

		try {
			accountManager.setAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, bggCookieJar.getAuthToken());
		} catch (SecurityException e) {
			showError("Uh-oh! This isn't an error we expect to see. If you have ScorePal installed, there's a known problem that one prevents the other from signing in. We're working to resolve the issue.");
			return;
		}
		Bundle userData = new Bundle();
		userData.putString(Authenticator.KEY_AUTH_TOKEN_EXPIRY, String.valueOf(bggCookieJar.getAuthTokenExpiry()));

		if (isRequestingNewAccount) {
			if (!accountManager.addAccountExplicitly(account, password, userData)) {
				Account existingAccount = Authenticator.getAccount(accountManager);
				if (existingAccount != null && existingAccount.name.equals(account.name)) {
					accountManager.setPassword(account, password);
				} else {
					passwordView.setError(getString(R.string.error_account_not_added));
					return;
				}
			}
		} else {
			accountManager.setPassword(account, password);
		}
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	@DebugLog
	private void showError(String message) {
		Builder b = new Builder(this);
		b.setTitle("Error").setMessage(message);
		b.create().show();
	}
}
