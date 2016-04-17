package com.boardgamegeek.io;

import com.boardgamegeek.model.Company;
import com.boardgamegeek.model.ForumListResponse;
import com.boardgamegeek.model.ForumResponse;
import com.boardgamegeek.model.GeekList;
import com.boardgamegeek.model.GeekListsResponse;
import com.boardgamegeek.model.HotnessResponse;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.SearchResponse;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.ThreadResponse;
import com.boardgamegeek.model.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface BoardGameGeekService {

	@GET("/xmlapi2/thing")
	Call<ThingResponse> thing(@Query("id") int gameId, @Query("stats") int stats);

	@GET("/xmlapi2/thing")
	Call<ThingResponse> thing(@Query("id") String gameIds, @Query("stats") int stats);

	@GET("/xmlapi2/thing?comments=1")
	Call<ThingResponse> thingWithComments(@Query("id") int gameId, @Query("page") int page);

	@GET("/xmlapi2/thing?ratingcomments=1")
	Call<ThingResponse> thingWithRatings(@Query("id") int gameId, @Query("page") int page);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> plays(@QueryMap Map<String, String> options);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> playsByDate(@Query("username") String username, @Query("mindate") String minDate, @Query("maxdate") String maxDate);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> playsByGame(@Query("username") String username, @Query("id") int gameId);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> plays(@Query("username") String username, @Query("id") int gameId, @Query("mindate") String minDate, @Query("maxdate") String maxDate);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> playsByMinDate(@Query("username") String username, @Query("mindate") String minDate, @Query("page") int page);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> playsByMaxDate(@Query("username") String username, @Query("maxdate") String maxDate, @Query("page") int page);

	@GET("/xmlapi2/plays")
	Call<PlaysResponse> plays(@Query("username") String username, @Query("page") int page);

	@GET("/xmlapi2/user")
	Call<User> user(@Query("name") String name);

	@GET("/xmlapi2/user")
	Call<User> user(@Query("name") String name, @Query("buddies") int buddies, @Query("page") int page);

	String PERSON_TYPE_ARTIST = "boardgameartist";
	String PERSON_TYPE_DESIGNER = "boardgamedesigner";

	@GET("/xmlapi/{type}/{id}")
	Call<Person> person(@Path("type") String type, @Path("id") int id);

	String COMPANY_TYPE_PUBLISHER = "boardgamepublisher";

	@GET("/xmlapi/{type}/{id}")
	Call<Company> company(@Path("type") String type, @Path("id") int id);

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

	@GET("/geeklist/module?ajax=1&domain=boardgame&nosession=1&showcount=12&tradelists=0&version=v2")
	Call<GeekListsResponse> geekLists(@Query("pageid") int page, @Query("sort") String sort);

	@GET("/xmlapi/geeklist/{id}?comments=1")
	Call<GeekList> geekList(@Path("id") int id);
}
