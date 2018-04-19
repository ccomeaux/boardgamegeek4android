package com.boardgamegeek.io;

import com.boardgamegeek.io.model.Image;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GeekdoApi {
	@GET("/api/images/{id}")
	Call<Image> image(@Path("id") int imageId);
}
