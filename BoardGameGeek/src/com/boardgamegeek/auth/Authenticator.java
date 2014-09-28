package com.boardgamegeek.auth;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGV;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;

import org.apache.http.client.CookieStore;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.NetworkErrorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.LoginActivity;
import com.boardgamegeek.util.HttpUtils;

public class Authenticator extends AbstractAccountAuthenticator {
	private static final String TAG = makeLogTag(Authenticator.class);

	public static final String ACCOUNT_TYPE = "com.boardgamegeek";
	public static final String AUTHTOKEN_TYPE = "com.boardgamegeek";
	public static final String KEY_AUTHTOKEN_EXPIRY = "AUTHTOKEN_EXPIRY";
	public static final String KEY_SESSION_ID = "SESSION_ID";
	public static final String KEY_SESSION_ID_EXPIRY = "SESSION_ID_EXPIRY";
	public static final String KEY_USER_ID = "com.boardgamegeek.USER_ID";

	private final Context mContext;

	public Authenticator(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
		String[] requiredFeatures, Bundle options) throws NetworkErrorException {
		LOGV(TAG, "Adding account: accountType=" + accountType + ", authTokenType=" + authTokenType);
		return createLoginIntent(response, null);
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
		throws NetworkErrorException {
		// TODO: is this needed? if so prompt for password only
		LOGV(TAG, "confirmCredentials - not supported");
		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		LOGV(TAG, "editProperties");
		throw new UnsupportedOperationException();
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
		Bundle options) throws NetworkErrorException {
		LOGV(TAG, "getting auth token...");

		// If the caller requested an authToken type we don't support, then return an error
		if (!authTokenType.equals(Authenticator.AUTHTOKEN_TYPE)) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
			return result;
		}

		final AccountManager am = AccountManager.get(mContext);

		// Return the cached auth token (unless expired)
		String authToken = am.peekAuthToken(account, authTokenType);
		if (!TextUtils.isEmpty(authToken)) {
			if (!isKeyExpired(am, account, KEY_AUTHTOKEN_EXPIRY)) {
				return createAuthTokenBundle(account, authToken);
			}
			am.invalidateAuthToken(authTokenType, authToken);
		}

		// Ensure the password is valid and not expired, then return the stored AuthToken
		final String password = am.getPassword(account);
		if (!TextUtils.isEmpty(password)) {
			CookieStore cs = HttpUtils.authenticate(account.name, password);
			AuthProfile ap = new AuthProfile(cs);
			am.setAuthToken(account, authTokenType, ap.authToken);
			am.setUserData(account, Authenticator.KEY_AUTHTOKEN_EXPIRY, String.valueOf(ap.authTokenExpiry));
			return createAuthTokenBundle(account, ap.authToken);
		}

		// If we get here, then we couldn't access the user's password - so we need to re-prompt them for their
		// credentials. We do that by creating an intent to display our AuthenticatorActivity panel.
		LOGI(TAG, "Expired credentials...");
		return createLoginIntent(response, account.name);
	}

	private Bundle createAuthTokenBundle(Account account, String authToken) {
		final Bundle result = new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
		result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
		return result;
	}

	private Bundle createLoginIntent(AccountAuthenticatorResponse response, String accountName) {
		final Intent intent = new Intent(mContext, LoginActivity.class);
		if (!TextUtils.isEmpty(accountName)) {
			intent.putExtra(LoginActivity.EXTRA_USERNAME, accountName);
		}
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	private boolean isKeyExpired(final AccountManager am, Account account, String key) {
		String expiration = am.getUserData(account, key);
		return !TextUtils.isEmpty(expiration) && Long.valueOf(expiration) < System.currentTimeMillis();
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		LOGV(TAG, "getAuthTokenLabel - we don't support multiple auth tokens");
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
		throws NetworkErrorException {
		LOGV(TAG, "hasFeatures - we don't support any features");
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
		Bundle options) throws NetworkErrorException {
		// TODO: is this needed? if so prompt for password only
		LOGV(TAG, "updateCredentials - not supported");
		return null;
	}

	@Override
	public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account)
		throws NetworkErrorException {
		LOGV(TAG, "getAccountRemovalAllowed - yes, always");
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
		return result;
	}

	public static Account getAccount(Context context) {
		return getAccount(AccountManager.get(context));
	}

	public static Account getAccount(AccountManager accountManager) {
		Account[] accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
		if (accounts == null || accounts.length == 0) {
			LOGW(TAG, "no account!");
			return null;
		} else if (accounts.length != 1) {
			LOGW(TAG, "multiple accounts!");
			return null;
		}
		return accounts[0];
	}

	public static String getUserId(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		String userId = accountManager.getUserData(account, KEY_USER_ID);
		if (userId == null) {
			return "0";
		}
		return userId;
	}

	public static boolean isSignedIn(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		return account != null;
	}

	public static void clearPassword(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		String authToken = accountManager.peekAuthToken(account, Authenticator.AUTHTOKEN_TYPE);
		if (authToken != null) {
			accountManager.invalidateAuthToken(Authenticator.AUTHTOKEN_TYPE, authToken);
		} else {
			accountManager.clearPassword(account);
		}
	}

	public static boolean isOldAuth(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		String data = accountManager.getUserData(account, "PASSWORD_EXPIRY");
		return data != null;
	}

	public static long getLong(Context context, String key) {
		return getLong(context, key, 0);
	}

	public static long getLong(Context context, String key, long defaultValue) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		if (account == null) {
			return defaultValue;
		}
		return getLong(accountManager, account, key, defaultValue);
	}

	public static long getLong(AccountManager accountManager, Account account, String key) {
		String s = accountManager.getUserData(account, key);
		return TextUtils.isEmpty(s) ? 0 : Long.parseLong(s);
	}

	public static long getLong(AccountManager accountManager, Account account, String key, long defaultValue) {
		String s = accountManager.getUserData(account, key);
		return TextUtils.isEmpty(s) ? defaultValue : Long.parseLong(s);
	}

	public static void putLong(Context context, String key, long value) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		accountManager.setUserData(account, key, String.valueOf(value));
	}

	public static void putInt(Context context, String key, int value) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		accountManager.setUserData(account, key, String.valueOf(value));
	}

	public static void signOut(final Context context) {
		AccountManager am = AccountManager.get(context);
		final Account account = Authenticator.getAccount(am);
		am.removeAccount(account, new AccountManagerCallback<Boolean>() {
			@Override
			public void run(AccountManagerFuture<Boolean> future) {
				if (future.isDone()) {
					try {
						if (future.getResult()) {
							Toast.makeText(context, R.string.msg_sign_out_success, Toast.LENGTH_LONG).show();
						}
					} catch (OperationCanceledException e) {
						LOGE(TAG, "removeAccount", e);
					} catch (IOException e) {
						LOGE(TAG, "removeAccount", e);
					} catch (AuthenticatorException e) {
						LOGE(TAG, "removeAccount", e);
					}
				}
			}
		}, null);
	}

	// StringBuilder sb = new StringBuilder();
	// sb.append("ACCOUNT").append("\n");
	// sb.append("Name:       ").append(account.name).append("\n");
	// sb.append("Type:       ").append(account.type).append("\n");
	// sb.append("Token type: ").append(authTokenType).append("\n");
	// sb.append("Password:   ").append(am.getPassword(account)).append("\n");
	// sb.append("Password X: ").append(new Date(Long.valueOf(am.getUserData(account, KEY_PASSWORD_EXPIRY))))
	// .append("\n");
	// sb.append("Session ID: ").append(am.getUserData(account, KEY_SESSION_ID)).append("\n");
	// sb.append("Session X:  ").append(new Date(Long.valueOf(am.getUserData(account, KEY_SESSION_ID_EXPIRY))))
	// .append("\n");
	// LOGI(TAG, sb.toString());
}
