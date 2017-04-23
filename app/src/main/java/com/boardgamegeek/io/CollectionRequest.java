package com.boardgamegeek.io;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class CollectionRequest {
	private final BggService bggService;
	private final String username;
	private final ArrayMap<String, String> options;

	public CollectionRequest(BggService service, String username, ArrayMap<String, String> options) {
		this.bggService = service;
		this.username = username;
		this.options = options;
	}

	@NonNull
	public CollectionResponse execute() {
		Call<com.boardgamegeek.model.CollectionResponse> call = bggService.collection(username, options);
		try {
			Response<com.boardgamegeek.model.CollectionResponse> response = call.execute();
			if (response.isSuccessful()) {
				return new CollectionResponse(response.body().items);
			} else {
				return new CollectionResponse("Unsuccessful collection fetch with code: %s", response.code());
			}
		} catch (IOException e) {
			return new CollectionResponse(e.getMessage());
		}
	}
}
