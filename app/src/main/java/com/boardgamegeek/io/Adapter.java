package com.boardgamegeek.io;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.auth.Authenticator;

import java.io.IOException;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RestAdapter.Builder;
import retrofit.RestAdapter.LogLevel;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.Response;
import retrofit.converter.Converter;

public class Adapter {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final int COLLECTION_REQUEST_PROCESSING = 202;
	private static final int API_RATE_EXCEEDED = 503;

	public static BggService create() {
		return createBuilder().build().create(BggService.class);
	}

	public static BggService createWithJson() {
		return createBuilderWithoutConverter().build().create(BggService.class);
	}

	public static BggService createWithAuth(Context context) {
		return addAuth(context, createBuilder()).build().create(BggService.class);
	}

	public static BggService createForPost(Context context, Converter converter) {
		return addAuth(context, createBuilder()).setConverter(converter).build().create(BggService.class);
	}

	private static Builder createBuilder() {
		return createBuilderWithoutConverter().setConverter(new BggXMLConverter());
	}

	private static Builder createBuilderWithoutConverter() {
		ErrorHandler errorHandler = new ErrorHandler() {
			@Override
			public Throwable handleError(RetrofitError cause) {
				Response r = cause.getResponse();
				if (r != null) {
					if (r.getStatus() == COLLECTION_REQUEST_PROCESSING) {
						return new RetryableException(cause);
					} else if (r.getStatus() == API_RATE_EXCEEDED) {
						return new RetryableException(cause);
					}
				}
				return cause;
			}
		};

		Builder builder = new RestAdapter.Builder()
			.setEndpoint("https://www.boardgamegeek.com/")
			.setErrorHandler(errorHandler);
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
			final String authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, true);
			requestInterceptor = new RequestInterceptor() {
				@Override
				public void intercept(RequestFacade request) {
					if (account != null && !TextUtils.isEmpty(account.name) && !TextUtils.isEmpty(authToken)) {
						request.addHeader("Cookie", "bggusername=" + account.name + "; bggpassword=" + authToken);
					}
				}
			};
		} catch (OperationCanceledException | AuthenticatorException | IOException e) {
			// TODO handle this somehow; maybe just return create()
		}

		if (requestInterceptor != null) {
			builder.setRequestInterceptor(requestInterceptor);
		}
		return builder;
	}
}
