package com.boardgamegeek.io;

import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.provider.BggContract;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class ThingRequest {
	private final BoardGameGeekService bggService;
	private final int id;
	private final String ids;

	public ThingRequest(BoardGameGeekService service, int id) {
		this.bggService = service;
		this.id = id;
		this.ids = null;
	}

	public ThingRequest(BoardGameGeekService service, String ids) {
		this.bggService = service;
		this.id = BggContract.INVALID_ID;
		this.ids = ids;
	}

	public ThingResponse execute() {
		try {
			Call<ThingResponse> call;
			if (id == BggContract.INVALID_ID) {
				call = bggService.thing(ids, 1);
			} else {
				call = bggService.thing(id, 1);
			}
			Response<ThingResponse> response = call.execute();
			if (response.isSuccessful()) {
				return response.body();
			} else {
				Timber.w("Unsuccessful thing fetch with code: %s", response.code());
			}
		} catch (IOException e) {
			Timber.w(e, "Unsuccessful thing fetch");
		}
		return null;
	}
}
