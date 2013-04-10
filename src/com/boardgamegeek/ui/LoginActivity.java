package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

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

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.VersionUtils;

/**
 * Activity which displays a login screen to the user, offering registration as well.
 */
public class LoginActivity extends AccountAuthenticatorActivity {
	private static final String TAG = makeLogTag(LoginActivity.class);

	public static final String EXTRA_USERNAME = "USERNAME";

	private String mUsername;
	private String mPassword;

	private EditText mUsernameView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;

	private UserLoginTask mAuthTask = null;
	private AccountManager mAccountManager;
	private boolean mRequestNewAccount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_login);

		mUsernameView = (EditText) findViewById(R.id.username);
		mPasswordView = (EditText) findViewById(R.id.password);
		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		mAccountManager = AccountManager.get(this);
		mUsername = getIntent().getStringExtra(EXTRA_USERNAME);

		mRequestNewAccount = mUsername == null;

		mUsernameView.setText(mUsername);
		mUsernameView.setEnabled(mRequestNewAccount);
		if (!mRequestNewAccount){
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
				mPasswordView
					.setInputType(isChecked ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
						: (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
			}
		});

		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
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
						mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
					}
				});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
					}
				});
		} else {
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate the user.
	 */
	public class UserLoginTask extends AsyncTask<Void, Void, CookieStore> {
		@Override
		protected CookieStore doInBackground(Void... params) {
			return HttpUtils.authenticate(mUsername, mPassword);
		}

		@Override
		protected void onPostExecute(final CookieStore cookieStore) {
			mAuthTask = null;
			showProgress(false);

			if (cookieStore != null) {
				createAccount(cookieStore);
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

	private void createAccount(CookieStore cs) {
		LOGI(TAG, "Creating account");
		final Account account = new Account(mUsername, BggApplication.ACCOUNT_TYPE);

		String password = null;
		Bundle userData = new Bundle();
		for (Cookie cookie : cs.getCookies()) {
			String name = cookie.getName();
			if (name.equals("bggpassword")) {
				password = cookie.getValue();
				userData.putString(Authenticator.KEY_PASSWORD_EXPIRY, String.valueOf(cookie.getExpiryDate().getTime()));
			} else if (name.equals("SessionID")) {
				userData.putString(Authenticator.KEY_SESSION_ID, cookie.getValue());
				userData.putString(Authenticator.KEY_SESSION_ID_EXPIRY,
					String.valueOf(cookie.getExpiryDate().getTime()));
			}
		}

		if (mRequestNewAccount) {
			if (!mAccountManager.addAccountExplicitly(account, password, userData)) {
				mPasswordView.setError(getString(R.string.error_account_not_added));
				return;
			}
		} else {
			mAccountManager.setPassword(account, password);
			mAccountManager.setUserData(account, Authenticator.KEY_PASSWORD_EXPIRY,
				userData.getString(Authenticator.KEY_PASSWORD_EXPIRY));
		}
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, BggApplication.ACCOUNT_TYPE);
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}
}
