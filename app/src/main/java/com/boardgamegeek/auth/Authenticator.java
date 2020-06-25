package com.boardgamegeek.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.NetworkErrorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.TextUtils;

import com.boardgamegeek.events.SignOutEvent;
import com.boardgamegeek.ui.LoginActivity;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class Authenticator extends AbstractAccountAuthenticator {
	public static final String ACCOUNT_TYPE = "com.boardgamegeek";
	public static final String AUTH_TOKEN_TYPE = "com.boardgamegeek";
	public static final String KEY_AUTH_TOKEN_EXPIRY = "AUTHTOKEN_EXPIRY";
	public static final String INVALID_USER_ID = "0";
	private static final String KEY_USER_ID = "com.boardgamegeek.USER_ID";

	private final Context context;

	public Authenticator(Context context) {
		super(context);
		this.context = context;
	}

	@NonNull
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
		Timber.v("Adding account: accountType=%s, authTokenType=%s", accountType, authTokenType);
		return LoginActivity.createIntentBundle(context, response, null);
	}

	@Nullable
	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
		Timber.v("confirmCredentials is not supported. If it was we would ask the user for their password.");
		return null;
	}

	@NonNull
	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		Timber.v("editProperties is not supported.");
		throw new UnsupportedOperationException();
	}

	@NonNull
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, @NonNull Account account, @NonNull String authTokenType, Bundle options) throws NetworkErrorException {
		Timber.v("getting auth token...");

		// If the caller requested an authToken type we don't support, then return an error
		if (!authTokenType.equals(Authenticator.AUTH_TOKEN_TYPE)) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
			return result;
		}

		final AccountManager am = AccountManager.get(context);

		// Return the cached auth token (unless expired)
		String authToken = am.peekAuthToken(account, authTokenType);
		if (!TextUtils.isEmpty(authToken)) {
			if (!isKeyExpired(am, account, KEY_AUTH_TOKEN_EXPIRY)) {
				Timber.v(toDebugString());
				return createAuthTokenBundle(account, authToken);
			}
			am.invalidateAuthToken(authTokenType, authToken);
		}

		// Ensure the password is valid and not expired, then return the stored AuthToken
		final String password = am.getPassword(account);
		if (!TextUtils.isEmpty(password)) {
			BggCookieJar cookieJar = NetworkAuthenticator.authenticate(account.name, password, "Renewal", context);
			if (cookieJar != null) {
				am.setAuthToken(account, authTokenType, cookieJar.getAuthToken());
				am.setUserData(account, Authenticator.KEY_AUTH_TOKEN_EXPIRY, String.valueOf(cookieJar.getAuthTokenExpiry()));
				Timber.v(toDebugString());
				return createAuthTokenBundle(account, cookieJar.getAuthToken());
			}
		}

		// If we get here, then we couldn't access the user's password - so we need to re-prompt them for their
		// credentials. We do that by creating an intent to display our AuthenticatorActivity panel.
		Timber.i("Expired credentials...");
		return LoginActivity.createIntentBundle(context, response, account.name);
	}

	@Nullable
	@Override
	public String getAuthTokenLabel(String authTokenType) {
		Timber.v("getAuthTokenLabel - we don't support multiple auth tokens");
		return null;
	}

	@NonNull
	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
		Timber.v("hasFeatures - we don't support any features");
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Nullable
	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
		Timber.v("updateCredentials is not supported. If it was we would ask the user for their password.");
		return null;
	}

	/**
	 * Gets the account associated with BoardGameGeek. Returns null if their is a problem getting the account.
	 */
	@Nullable
	public static Account getAccount(Context context) {
		if (context != null) {
			return getAccount(AccountManager.get(context));
		}
		return null;
	}

	/**
	 * Gets the account associated with BoardGameGeek. Returns null if their is a problem getting the account.
	 */
	@Nullable
	public static Account getAccount(@NonNull AccountManager accountManager) {
		Account[] accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
		if (accounts.length == 0) {
			// likely the user has never signed in
			Timber.v("no account!");
			return null;
		} else if (accounts.length != 1) {
			Timber.w("multiple accounts!");
			return null;
		}
		return accounts[0];
	}

	/**
	 * Get the BGG user ID of the authenticated user.
	 */
	public static String getUserId(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		if (account == null) return INVALID_USER_ID;
		String userId = accountManager.getUserData(account, KEY_USER_ID);
		if (userId == null) return INVALID_USER_ID;
		return userId;
	}

	public static void putUserId(Context context, int value) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		if (account != null) {
			accountManager.setUserData(account, KEY_USER_ID, String.valueOf(value));
		}
	}

	/**
	 * Determines if the user is signed in.
	 */
	public static boolean isSignedIn(Context context) {
		return getAccount(context) != null;
	}

	public static void clearPassword(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		if (account != null) {
			String authToken = accountManager.peekAuthToken(account, Authenticator.AUTH_TOKEN_TYPE);
			if (authToken != null) {
				accountManager.invalidateAuthToken(Authenticator.AUTH_TOKEN_TYPE, authToken);
			} else {
				accountManager.clearPassword(account);
			}
		}
	}

	public static boolean isOldAuth(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = getAccount(accountManager);
		String data = accountManager.getUserData(account, "PASSWORD_EXPIRY");
		return data != null;
	}

	public static void signOut(final Context context) {
		AccountManager am = AccountManager.get(context);
		final Account account = Authenticator.getAccount(am);
		if (account != null) {
			removeAccountCompat(am, account, true);
			am.setUserData(account, KEY_USER_ID, INVALID_USER_ID);
		}
		AccountUtils.clearFields(context);
	}

	public static void removeAccounts(final Context context) {
		AccountManager am = AccountManager.get(context);
		Account[] accounts = am.getAccountsByType(Authenticator.ACCOUNT_TYPE);
		for (Account account : accounts) {
			removeAccountCompat(am, account, false);
			am.setUserData(account, KEY_USER_ID, INVALID_USER_ID);
		}
	}

	@NonNull
	private Bundle createAuthTokenBundle(@NonNull Account account, String authToken) {
		final Bundle result = new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
		result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
		return result;
	}

	private boolean isKeyExpired(@NonNull final AccountManager am, Account account, @SuppressWarnings("SameParameterValue") String key) {
		String expiration = am.getUserData(account, key);
		return !TextUtils.isEmpty(expiration) && Long.valueOf(expiration) < System.currentTimeMillis();
	}

	private static void removeAccountCompat(AccountManager am, Account account, boolean postEvent) {
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP_MR1) {
			removeAccountWithActivity(am, account, postEvent);
		} else {
			removeAccount(am, account, postEvent);
		}
	}

	@SuppressWarnings("deprecation")
	private static void removeAccount(@NonNull AccountManager am, Account account, final boolean postEvent) {
		am.removeAccount(account, new AccountManagerCallback<Boolean>() {
			@Override
			public void run(@NonNull AccountManagerFuture<Boolean> future) {
				if (future.isDone()) {
					try {
						if (postEvent && future.getResult()) EventBus.getDefault().post(new SignOutEvent());
					} catch (@NonNull OperationCanceledException | AuthenticatorException | IOException e) {
						Timber.e(e, "removeAccount");
					}
				}
			}
		}, null);
	}

	@TargetApi(VERSION_CODES.LOLLIPOP_MR1)
	private static void removeAccountWithActivity(@NonNull AccountManager am, Account account, final boolean postEvent) {
		am.removeAccount(account, null, new AccountManagerCallback<Bundle>() {
			@Override
			public void run(@NonNull AccountManagerFuture<Bundle> future) {
				if (future.isDone()) {
					try {
						if (postEvent && future.getResult().getBoolean(AccountManager.KEY_BOOLEAN_RESULT)) {
							EventBus.getDefault().post(new SignOutEvent());
						}
					} catch (@NonNull OperationCanceledException | AuthenticatorException | IOException e) {
						Timber.e(e, "removeAccount");
					}
				}
			}
		}, null);
	}

	@NonNull
	private String toDebugString() {
		if (context == null) {
			return "";
		}
		AccountManager accountManager = AccountManager.get(context);
		if (accountManager == null) {
			return "";
		}
		Account account = getAccount(accountManager);
		if (account == null) {
			return "";
		}
		return "ACCOUNT" + "\n" +
			"Name:       " + account.name + "\n" +
			"Type:       " + account.type + "\n" +
			"Password:   " + accountManager.getPassword(account) + "\n";
	}
}
