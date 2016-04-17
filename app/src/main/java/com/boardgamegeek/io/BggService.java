package com.boardgamegeek.io;

import com.boardgamegeek.model.PlayPostResponse;

import java.util.Map;

import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;

public interface BggService {

	@FormUrlEncoded
	@POST("/geekplay.php")
	PlayPostResponse geekPlay(@FieldMap Map<String, String> form);
}
