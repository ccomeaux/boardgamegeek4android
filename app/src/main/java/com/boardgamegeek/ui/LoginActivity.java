package com.boardgamegeek.ui;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog.Builder;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AuthResponse;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.auth.NetworkAuthenticator;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.VersionUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Activity which displays a login screen to the user, offering registration as well.
 */
public class LoginActivity extends AccountAuthenticatorActivity {
	private String mUsername;
	private String mPassword;

	@InjectView(R.id.username) EditText mUsernameView;
	@InjectView(R.id.password) EditText mPasswordView;
	@InjectView(R.id.login_form) View mLoginFormView;
	@InjectView(R.id.login_status) View mLoginStatusView;
	@InjectView(R.id.login_status_message) TextView mLoginStatusMessageView;

	private UserLoginTask mAuthTask = null;
	private AccountManager mAccountManager;
	private boolean mRequestNewAccount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		ButterKnife.inject(this);

		mAccountManager = AccountManager.get(this);
		mUsername = getIntent().getStringExtra(ActivityUtils.KEY_USER);

		mRequestNewAccount = mUsername == null;

		mUsernameView.setText(mUsername);
		mUsernameView.setEnabled(mRequestNewAccount);
		if (!mRequestNewAccount) {
			mPasswordView.requestFocus();
		}

		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
				if (actionId == R.id.login || actionId == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		((CheckBox) findViewById(R.id.show_password)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				int selectionStart = mPasswordView.getSelectionStart();
				int selectionEnd = mPasswordView.getSelectionEnd();
				mPasswordView
					.setInputType(isChecked ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
						: (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
				mPasswordView.setSelection(selectionStart, selectionEnd);
			}
		});
	}

	@Override
	public void onBackPressed() {
		if (mAuthTask != null) {
			mAuthTask.cancel(true);
		} else {
			super.onBackPressed();
		}
	}

	@OnClick(R.id.sign_in_button)
	public void onSignInClick(View view) {
		attemptLogin();
	}

	/**
	 * Attempts to sign in or register the account specified by the login form. If there are form errors (invalid email,
	 * missing fields, etc.), the errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUsernameView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		if (mRequestNewAccount) {
			mUsername = mUsernameView.getText().toString();
		}
		mPassword = mPasswordView.getText().toString();

		View focusView = null;

		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
		}

		if (TextUtils.isEmpty(mUsername)) {
			mUsernameView.setError(getString(R.string.error_field_required));
			focusView = mUsernameView;
		}

		if (focusView != null) {
			// There was an error; don't attempt login and focus the first form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserLoginTask();
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// Fade in/out if possible
		if (VersionUtils.hasHoneycombMR2()) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mLoginStatusView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
					}
				});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mLoginFormView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
					}
				});
		} else {
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
			mLoginFormView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, AuthResponse> {
		@Override
		protected AuthResponse doInBackground(Void... params) {
			return NetworkAuthenticator.authenticate(mUsername, mPassword);
		}

		@Override
		protected void onPostExecute(AuthResponse authResponse) {
			mAuthTask = null;
			showProgress(false);

			if (authResponse != null) {
				createAccount(authResponse);
			} else {
				mPasswordView.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}

	private void createAccount(AuthResponse authResponse) {
		Timber.i("Creating account");
		final Account account = new Account(mUsername, Authenticator.ACCOUNT_TYPE);

		try {
			mAccountManager.setAuthToken(account, Authenticator.AUTHTOKEN_TYPE, authResponse.authToken);
		} catch (SecurityException e) {
			showError("Uh-oh! This isn't an error we expect to see. If you have ScorePal installed, there's a known problem that one prevents the other from signing in. We're working to resolve the issue.");
			return;
		}
		Bundle userData = new Bundle();
		userData.putString(Authenticator.KEY_AUTHTOKEN_EXPIRY, String.valueOf(authResponse.authTokenExpiry));

		if (mRequestNewAccount) {
			if (!mAccountManager.addAccountExplicitly(account, mPassword, userData)) {
				Account existingAccount = Authenticator.getAccount(mAccountManager);
				if (existingAccount != null && existingAccount.name.equals(account.name)) {
					mAccountManager.setPassword(account, mPassword);
				} else {
					mPasswordView.setError(getString(R.string.error_account_not_added));
					return;
				}
			}
		} else {
			mAccountManager.setPassword(account, mPassword);
		}
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	private void showError(String message) {
		Builder b = new Builder(this);
		b.setTitle("Error").setMessage(message);
		b.create().show();
	}
}
