package com.boardgamegeek.io;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.auth.Authenticator;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RestAdapter.Builder;
import retrofit.RestAdapter.LogLevel;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
import retrofit.converter.Converter;
import retrofit.converter.SimpleXMLConverter;

public class Adapter {
	private static final boolean DEBUG = BuildConfig.DEBUG;

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
		return createBuilderWithoutConverter().setConverter(new SimpleXMLConverter(false));
	}

	private static Builder createBuilderWithoutConverter() {
		OkHttpClient client = new OkHttpClient();
		client.interceptors().add(new RetryInterceptor());

		Builder builder = new RestAdapter.Builder()
			.setEndpoint("https://www.boardgamegeek.com/")
			.setClient(new OkClient(client));
		if (DEBUG) {
			builder.setLog(new AndroidLog("BGG-retrofit")).setLogLevel(LogLevel.FULL);
		}

		return builder;
	}

	private static Builder addAuth(Context context, Builder builder) {
		RequestInterceptor requestInterceptor = null;

		AccountManager accountManager = AccountManager.get(context);
		final Account account = Authenticator.getAccount(accountManager);
		if (account != null) {
			try {
				final String authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, true);
				requestInterceptor = new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						if (!TextUtils.isEmpty(account.name) && !TextUtils.isEmpty(authToken)) {
							request.addHeader("Cookie", "bggusername=" + account.name + "; bggpassword=" + authToken);
						}
					}
				};
			} catch (OperationCanceledException | AuthenticatorException | IOException e) {
				// TODO handle this somehow; maybe just return create()
			}
		}

		if (requestInterceptor != null) {
			builder.setRequestInterceptor(requestInterceptor);
		}
		return builder;
	}
}
