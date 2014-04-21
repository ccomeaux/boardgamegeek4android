package com.boardgamegeek.io;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

import com.boardgamegeek.model.Company;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.model.SearchResponse;

public interface BggService {
	public static final String PERSON_TYPE_ARTIST = "boardgameartist";
	public static final String PERSON_TYPE_DESIGNER = "boardgamedesigner";
	public static final String COMPANY_TYPE_PUBLISHER = "boardgamepublisher";
	public static final String SEARCH_TYPE_BOARD_GAME = "boardgame";
	public static final String SEARCH_TYPE_BOARD_GAME_EXPANSION = "boardgameexpansion";
	public static final String SEARCH_TYPE_RPG = "rpg";
	public static final String SEARCH_TYPE_RPG_ITEM = "rpgitem";
	public static final String SEARCH_TYPE_VIDEO_GAME = "videogame";
	// other search types: boardgameartist, boardgamedesigner, boardgamepublisher

	@GET("/xmlapi/{type}/{id}")
	Person person(@Path("type") String type, @Path("id") int id);

	@GET("/xmlapi/{type}/{id}")
	Company company(@Path("type") String type, @Path("id") int id);

	@GET("/xmlapi2/search")
	SearchResponse search(@Query("query") String query, @Query("type") String type, @Query("exact") int exact);
}
