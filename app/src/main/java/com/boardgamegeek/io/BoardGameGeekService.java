package com.boardgamegeek.io;

import com.boardgamegeek.model.HotnessResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BoardGameGeekService {
	String HOTNESS_TYPE_BOARDGAME = "boardgame";

	@GET("/xmlapi2/hot")
	Call<HotnessResponse> getHotness(@Query("type") String type);
}
