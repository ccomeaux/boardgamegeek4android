package com.boardgamegeek.auth;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGV;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import org.apache.http.client.CookieStore;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.ui.LoginActivity;
import com.boardgamegeek.util.HttpUtils;

public class Authenticator extends AbstractAccountAuthenticator {
	private static final String TAG = makeLogTag(Authenticator.class);

	public static final String KEY_AUTHTOKEN_EXPIRY = "AUTHTOKEN_EXPIRY";
	public static final String KEY_SESSION_ID = "SESSION_ID";
	public static final String KEY_SESSION_ID_EXPIRY = "SESSION_ID_EXPIRY";

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
		if (!authTokenType.equals(BggApplication.AUTHTOKEN_TYPE)) {
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
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, BggApplication.ACCOUNT_TYPE);
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
		Account[] accounts = accountManager.getAccountsByType(BggApplication.ACCOUNT_TYPE);
		if (accounts == null || accounts.length == 0) {
			LOGW(TAG, "no account!");
			return null;
		} else if (accounts.length != 1) {
			LOGW(TAG, "multiple accounts!");
			return null;
		}
		return accounts[0];
	}

	public static boolean isSignedIn(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		if (account != null) {
			return !TextUtils.isEmpty(accountManager.getPassword(account));
		}
		return false;
	}

	public static void clearPassword(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		accountManager.clearPassword(account);
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
