package com.boardgamegeek.io;

import java.io.IOException;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.xml.SimpleXMLConverter;

public class Adapter {
	public static RestAdapter get() {
		return new RestAdapter.Builder().setEndpoint("http://www.boardgamegeek.com/")
			.setConverter(new SimpleXMLConverter()).build();
	}

	public static BggService create() {
		return new RestAdapter.Builder().setEndpoint("http://www.boardgamegeek.com/")
			.setConverter(new SimpleXMLConverter()).build().create(BggService.class);
	}

	public static BggService createWithAuth(Context context) {
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

		return new RestAdapter.Builder().setEndpoint("http://www.boardgamegeek.com/")
			.setRequestInterceptor(requestInterceptor).setConverter(new SimpleXMLConverter()).build()
			.create(BggService.class);
	}
}
