package com.boardgamegeek.io;

import com.boardgamegeek.model.ForumListResponse;
import com.boardgamegeek.model.ForumResponse;
import com.boardgamegeek.model.HotnessResponse;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.ThreadResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BoardGameGeekService {

	@GET("/xmlapi2/thing?comments=1")
	Call<ThingResponse> thingWithComments(@Query("id") int gameId, @Query("page") int page);

	@GET("/xmlapi2/thing?ratingcomments=1")
	Call<ThingResponse> thingWithRatings(@Query("id") int gameId, @Query("page") int page);


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
}
