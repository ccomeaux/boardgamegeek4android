package com.boardgamegeek.io;

import com.boardgamegeek.io.model.CollectionResponse;
import com.boardgamegeek.io.model.CompanyResponse2;
import com.boardgamegeek.io.model.ForumListResponse;
import com.boardgamegeek.io.model.ForumResponse;
import com.boardgamegeek.io.model.GeekListResponse;
import com.boardgamegeek.io.model.GeekListsResponse;
import com.boardgamegeek.io.model.HotnessResponse;
import com.boardgamegeek.io.model.Person;
import com.boardgamegeek.io.model.PersonResponse2;
import com.boardgamegeek.io.model.PlaysResponse;
import com.boardgamegeek.io.model.SearchResponse;
import com.boardgamegeek.io.model.ThingResponse;
import com.boardgamegeek.io.model.ThreadResponse;
import com.boardgamegeek.io.model.User;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface BggService {
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

	@GET("/xmlapi2/collection")
	Call<CollectionResponse> collection(@Query("username") String username, @QueryMap Map<String, String> options);

	@GET("/xmlapi2/thing")
	Call<ThingResponse> thing(@Query("id") int gameId, @Query("stats") int stats);

	@GET("/xmlapi2/thing")
	Call<ThingResponse> thing(@Query("id") String gameIds, @Query("stats") int stats);

	@GET("/xmlapi2/thing?comments=1")
	Call<ThingResponse> thingWithComments(@Query("id") int gameId, @Query("page") int page);

	@GET("/xmlapi2/thing?ratingcomments=1")
	Call<ThingResponse> thingWithRatings(@Query("id") int gameId, @Query("page") int page);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> playsByDate(@Query("username") String username, @Query("mindate") String minDate, @Query("maxdate") String maxDate, @Query("page") int page);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> playsByGame(@Query("username") String username, @Query("id") int gameId, @Query("page") int page);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> plays(@Query("username") String username, @Query("mindate") String minDate, @Query("maxdate") String maxDate, @Query("page") int page);

	@GET("/xmlapi2/user")
	Call<User> user(@Query("name") String name);

	@GET("/xmlapi2/user")
	Call<User> user(@Query("name") String name, @Query("buddies") int buddies, @Query("page") int page);

	String PERSON_TYPE_ARTIST = "boardgameartist";
	String PERSON_TYPE_DESIGNER = "boardgamedesigner";

	@GET("/xmlapi/{type}/{id}")
	Call<Person> person(@Path("type") String type, @Path("id") int id);

	@GET("/xmlapi2/person")
	Call<PersonResponse2> person(@Query("id") int id);

	@GET("/xmlapi2/company/{id}")
	Call<CompanyResponse2> company(@Query("id") int id);

	String SEARCH_TYPE_BOARD_GAME = "boardgame";
	String SEARCH_TYPE_BOARD_GAME_EXPANSION = "boardgameexpansion";
	String SEARCH_TYPE_RPG = "rpg";
	String SEARCH_TYPE_RPG_ITEM = "rpgitem";
	String SEARCH_TYPE_VIDEO_GAME = "videogame";
	// other search types: boardgameartist, boardgamedesigner, boardgamepublisher

	@GET("/xmlapi2/search")
	Call<SearchResponse> search(@Query("query") String query, @Query("type") String type, @Query("exact") int exact);

	String HOTNESS_TYPE_BOARDGAME = "boardgame";

	@GET("/xmlapi2/hot")
	Call<HotnessResponse> getHotness(@Query("type") String type);

	String FORUM_TYPE_REGION = "region";
	String FORUM_TYPE_THING = "thing";
	String FORUM_TYPE_PERSON = "person";
	String FORUM_TYPE_COMPANY = "company";

	int FORUM_REGION_BOARDGAME = 1;
	int FORUM_REGION_RPG = 2;
	int FORUM_REGION_VIDEOGAME = 3;

	@GET("/xmlapi2/forumlist")
	Call<ForumListResponse> forumList(@Query("type") String type, @Query("id") int id);

	@GET("/xmlapi2/forum")
	Call<ForumResponse> forum(@Query("id") int id, @Query("page") int page);

	@GET("/xmlapi2/thread")
	Call<ThreadResponse> thread(@Query("id") int id);

	String GEEK_LIST_SORT_HOT = "hot";
	String GEEK_LIST_SORT_RECENT = "recent";
	String GEEK_LIST_SORT_ACTIVE = "active";

	@GET("/geeklist/module?ajax=1&domain=boardgame&nosession=1&tradelists=0&version=v5")
	Call<GeekListsResponse> geekLists(@Query("sort") String sort, @Query("showcount") int pageSize, @Query("pageid") int page);

	@GET("/xmlapi/geeklist/{id}")
	Call<GeekListResponse> geekList(@Path("id") int id, @Query("comments") int comments);
}
