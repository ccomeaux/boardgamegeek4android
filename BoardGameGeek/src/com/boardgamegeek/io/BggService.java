package com.boardgamegeek.io;

import retrofit.http.GET;
import retrofit.http.Path;

import com.boardgamegeek.model.Company;
import com.boardgamegeek.model.Person;

public interface BggService {
	public static final String PERSON_TYPE_ARTIST = "boardgameartist";
	public static final String PERSON_TYPE_DESIGNER = "boardgamedesigner";
	public static final String COMPANY_TYPE_PUBLISHER = "boardgamepublisher";

	@GET("/xmlapi/{type}/{id}")
	Person person(@Path("type") String type, @Path("id") int id);

	@GET("/xmlapi/{type}/{id}")
	Company company(@Path("type") String type, @Path("id") int id);
}
