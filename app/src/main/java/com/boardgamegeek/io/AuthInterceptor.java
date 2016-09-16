package com.boardgamegeek.io;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.util.HttpUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
	private final Context context;

	public AuthInterceptor(Context context) {
		this.context = context;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();

		AccountManager accountManager = AccountManager.get(context);
		final Account account = Authenticator.getAccount(accountManager);
		if (account != null) {
			final String authToken;
			try {
				authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, true);
				if (!TextUtils.isEmpty(account.name) && !TextUtils.isEmpty(authToken)) {
					final String cookieValue = "bggusername=" + HttpUtils.encode(account.name) + "; bggpassword=" + authToken;
					Request request = originalRequest.newBuilder().addHeader("Cookie", cookieValue).build();
					return chain.proceed(request);
				}
			} catch (OperationCanceledException | AuthenticatorException ignored) {
			}
		}
		return chain.proceed(originalRequest);
	}

}
