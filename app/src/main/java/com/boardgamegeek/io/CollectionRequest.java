package com.boardgamegeek.io;

import android.support.v4.util.ArrayMap;

import com.boardgamegeek.model.CollectionResponse;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class CollectionRequest {
	private final BoardGameGeekService bggService;
	private final String username;
	private final ArrayMap<String, String> options;

	public CollectionRequest(BoardGameGeekService service, String username, ArrayMap<String, String> options) {
		this.bggService = service;
		this.username = username;
		this.options = options;
	}

	public CollectionResponse execute() {
		Call<CollectionResponse> call = bggService.collection(username, options);
		try {
			Response<CollectionResponse> response = call.execute();
			if (response.isSuccessful()) {
				return response.body();
			} else {
				Timber.w("Unsuccessful collection fetch with code: %s", response.code());
			}
		} catch (IOException e) {
			Timber.w(e, "Unsuccessful collection fetch");
		}
		return null;
	}
}
