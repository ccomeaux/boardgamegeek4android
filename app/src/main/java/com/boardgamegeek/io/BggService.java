package com.boardgamegeek.io;

import com.boardgamegeek.model.CollectionCommentPostResponse;
import com.boardgamegeek.model.CollectionRatingPostResponse;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.PlayPostResponse;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.ThingResponse;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import retrofit.http.FieldMap;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;
import retrofit.http.QueryMap;

public interface BggService {
	// rpg
	// videogame
	// boardgameperson
	// rpgperson
	// boardgamecompany
	// rpgcompany
	// videogamecompany
	String THING_SUBTYPE_BOARDGAME = "boardgame";
	String THING_SUBTYPE_BOARDGAME_EXPANSION = "boardgameexpansion";
	String THING_SUBTYPE_BOARDGAME_ACCESSORY = "boardgameaccessory";

	String RANK_TYPE_SUBTYPE = "subtype";
	String RANK_TYPE_FAMILY = "family";
	String RANK_FAMILY_NAME_ABSTRACT_GAMES = "abstracts";
	String RANK_FAMILY_NAME_CUSTOMIZABLE_GAMES = "cgs";
	String RANK_FAMILY_NAME_CHILDRENS_GAMES = "childrensgames";
	String RANK_FAMILY_NAME_FAMILY_GAMES = "familygames";
	String RANK_FAMILY_NAME_PARTY_GAMES = "partygames";
	String RANK_FAMILY_NAME_STRATEGY_GAMES = "strategygames";
	String RANK_FAMILY_NAME_THEMATIC_GAMES = "thematic";
	String RANK_FAMILY_NAME_WAR_GAMES = "wargames";

	String COLLECTION_QUERY_KEY_ID = "id";
	String COLLECTION_QUERY_KEY_SHOW_PRIVATE = "showprivate";
	String COLLECTION_QUERY_KEY_STATS = "stats";
	String COLLECTION_QUERY_KEY_MODIFIED_SINCE = "modifiedsince";
	String COLLECTION_QUERY_KEY_BRIEF = "brief";
	String COLLECTION_QUERY_KEY_SUBTYPE = "subtype";
	String COLLECTION_QUERY_STATUS_PLAYED = "played";
	SimpleDateFormat COLLECTION_QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	SimpleDateFormat COLLECTION_QUERY_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

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

	@GET("/xmlapi2/plays")
	PlaysResponse playsByDate(@Query("username") String username, @Query("mindate") String minDate, @Query("maxdate") String maxDate);

	@GET("/xmlapi2/plays")
	PlaysResponse playsByGame(@Query("username") String username, @Query("id") int gameId);

	@GET("/xmlapi2/plays")
	PlaysResponse plays(@Query("username") String username, @Query("id") int gameId, @Query("mindate") String minDate, @Query("maxdate") String maxDate);

	@GET("/xmlapi2/plays")
	PlaysResponse playsByMinDate(@Query("username") String username, @Query("mindate") String minDate, @Query("page") int page);

	@GET("/xmlapi2/plays")
	PlaysResponse playsByMaxDate(@Query("username") String username, @Query("maxdate") String maxDate, @Query("page") int page);

	@GET("/xmlapi2/plays")
	PlaysResponse plays(@Query("username") String username, @Query("page") int page);

	@GET("/xmlapi2/collection")
	CollectionResponse collection(@Query("username") String username, @QueryMap Map<String, String> options);

	@FormUrlEncoded
	@POST("/geekcollection.php")
	CollectionRatingPostResponse geekCollectionRating(@FieldMap Map<String, String> form);

	@FormUrlEncoded
	@POST("/geekcollection.php")
	CollectionCommentPostResponse geekCollectionComment(@FieldMap Map<String, String> form);

	@GET("/xmlapi2/thing")
	ThingResponse thing(@Query("id") int gameId, @Query("stats") int stats);

	@GET("/xmlapi2/thing")
	ThingResponse thing(@Query("id") String gameIds, @Query("stats") int stats);

	@FormUrlEncoded
	@POST("/geekplay.php")
	PlayPostResponse geekPlay(@FieldMap Map<String, String> form);
}
