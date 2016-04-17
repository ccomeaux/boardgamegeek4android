package com.boardgamegeek.io;

import com.boardgamegeek.model.PlaysResponse;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class PlaysRequest {
	private static final int TYPE_ALL = 0;
	public static final int TYPE_MIN = 1;
	public static final int TYPE_MAX = 2;

	private final BoardGameGeekService bggService;
	private final int type;
	private final String username;
	private final int page;
	private final String date;

	public PlaysRequest(BoardGameGeekService service, int type, String username, int page, String date) {
		this.bggService = service;
		this.type = type;
		this.username = username;
		this.date = date;
		this.page = page;
	}

	public PlaysRequest(BoardGameGeekService service, String username, int page) {
		this.bggService = service;
		this.type = TYPE_ALL;
		this.username = username;
		this.page = page;
		this.date = "";
	}

	public PlaysResponse execute() {
		Call<PlaysResponse> call;
		if (type == PlaysRequest.TYPE_MIN) {
			call = bggService.playsByMinDate(username, date, page);
		} else if (type == PlaysRequest.TYPE_MAX) {
			call = bggService.playsByMaxDate(username, date, page);
		} else {
			call = bggService.plays(username, page);
		}
		try {
			Response<PlaysResponse> response = call.execute();
			if (response.isSuccessful()) {
				return response.body();
			} else {
				Timber.w("Unsuccessful plays fetch with code: %s", response.code());
			}
		} catch (Exception e) {
			Timber.w(e, "Unsuccessful plays fetch");
		}
		return null;
	}
}
