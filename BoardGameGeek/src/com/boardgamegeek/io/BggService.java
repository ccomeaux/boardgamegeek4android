package com.boardgamegeek.io;

import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;

import com.boardgamegeek.model.Company;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.model.PlaysResponse;
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
	
	// username=NAME Name of the player you want to request play information for. Data is returned in
	// backwards-chronological form. You must include either a username or an id and type to get results.
	// id=NNN Id number of the item you want to request play information for. Data is returned in
	// backwards-chronological form.
	// type=TYPE Type of the item you want to request play information for. Valid types include:
	// thing
	// family
	// mindate=YYYY-MM-DD Returns only plays of the specified date or later.
	// maxdate=YYYY-MM-DD Returns only plays of the specified date or earlier.
	// subtype=TYPE Limits play results to the specified TYPE; boardgame is the default. Valid types include:
	// boardgame
	// boardgameexpansion
	// rpgitem
	// videogame
	// page=NNN The page of information to request. Page size is 100 records.
	@GET("/xmlapi2/plays")
	PlaysResponse plays(@QueryMap Map<String, String> options);
}
