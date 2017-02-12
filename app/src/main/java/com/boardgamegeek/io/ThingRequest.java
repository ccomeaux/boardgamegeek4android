package com.boardgamegeek.io;

import com.boardgamegeek.provider.BggContract;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class ThingRequest {
	private final BggService bggService;
	private final int id;
	private final String ids;

	public ThingRequest(BggService service, int id) {
		this.bggService = service;
		this.id = id;
		this.ids = null;
	}

	public ThingRequest(BggService service, String ids) {
		this.bggService = service;
		this.id = BggContract.INVALID_ID;
		this.ids = ids;
	}

	public ThingResponse execute() {
		Call<com.boardgamegeek.model.ThingResponse> call;
		if (id == BggContract.INVALID_ID) {
			call = bggService.thing(ids, 1);
		} else {
			call = bggService.thing(id, 1);
		}
		try {
			Response<com.boardgamegeek.model.ThingResponse> response = call.execute();
			if (response.isSuccessful()) {
				return new ThingResponse(response.body().getGames());
			} else {
				return new ThingResponse("Unsuccessful thing fetch with code: %s", response.code());
			}
		} catch (IOException e) {
			return new ThingResponse(e.getMessage());
		}
	}
}
