package com.boardgamegeek.io;

import android.content.Context;

import com.boardgamegeek.util.HttpUtils;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class Adapter {
	public static BggService createForXml() {
		Retrofit.Builder builder = createBuilderWithoutConverterFactory(null);
		builder.addConverterFactory(SimpleXmlConverterFactory.createNonStrict());
		return builder.build().create(BggService.class);
	}

	public static BggService createForXmlWithAuth(Context context) {
		Retrofit.Builder builder = createBuilderWithoutConverterFactory(context);
		builder.addConverterFactory(SimpleXmlConverterFactory.createNonStrict());
		return builder.build().create(BggService.class);
	}

	public static BggService createForJson() {
		Retrofit.Builder builder = createBuilderWithoutConverterFactory(null);
		builder.addConverterFactory(GsonConverterFactory.create());
		return builder.build().create(BggService.class);
	}

	private static Retrofit.Builder createBuilderWithoutConverterFactory(Context context) {
		okhttp3.OkHttpClient httpClient;
		if (context == null) {
			httpClient = HttpUtils.getHttpClient();
		} else {
			httpClient = HttpUtils.getHttpClientWithAuth(context);
		}
		Retrofit.Builder builder = new Retrofit.Builder()
			.baseUrl("https://www.boardgamegeek.com/")
			.client(httpClient);
		return builder;
	}
}
