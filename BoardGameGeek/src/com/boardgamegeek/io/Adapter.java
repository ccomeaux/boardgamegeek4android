package com.boardgamegeek.io;

import java.io.IOException;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RestAdapter.Builder;
import retrofit.RestAdapter.LogLevel;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.Response;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;

public class Adapter {
	private static final boolean DEBUG = false;

	public static BggService create() {
		return createBuilder().build().create(BggService.class);
	}

	public static BggService createWithAuth(Context context) {
		return addAuth(context, createBuilder()).build().create(BggService.class);
	}

	public static BggService createForPost(Context context) {
		return addAuth(context, createBuilder()).setConverter(new JsonConverter()).build().create(BggService.class);
	}

	private static Builder createBuilder() {
		ErrorHandler errorHandler = new ErrorHandler() {
			@Override
			public Throwable handleError(RetrofitError cause) {
				Response r = cause.getResponse();
				if (r != null && r.getStatus() == 202) {
					return new RetryableException(cause);
				}
				return cause;
			}
		};

		Builder builder = new RestAdapter.Builder().setEndpoint("http://www.boardgamegeek.com/")
			.setConverter(new SimpleXMLConverter()).setErrorHandler(errorHandler);
		if (DEBUG) {
			builder.setLog(new AndroidLog("BGG-retrofit")).setLogLevel(LogLevel.FULL);
		}
		return builder;
	}

	private static Builder addAuth(Context context, Builder builder) {
		RequestInterceptor requestInterceptor = null;

		AccountManager accountManager = AccountManager.get(context);
		final Account account = Authenticator.getAccount(accountManager);
		try {
			final String authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTHTOKEN_TYPE, true);
			requestInterceptor = new RequestInterceptor() {
				@Override
				public void intercept(RequestFacade request) {
					request.addHeader("Cookie", "bggusername=" + account.name + "; bggpassword=" + authToken);
				}
			};
		} catch (OperationCanceledException | AuthenticatorException | IOException e) {
			// TODO handle this somehow; maybe just return create()
		}

		return builder.setRequestInterceptor(requestInterceptor);
	}
}
