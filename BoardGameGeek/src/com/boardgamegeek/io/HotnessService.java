package com.boardgamegeek.io;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import com.boardgamegeek.model.HotGame;

import retrofit.http.GET;
import retrofit.http.Query;

public interface HotnessService {
	public static final String TYPE_BOARDGAME = "boardgame";

	// rpg
	// videogame
	// boardgameperson
	// rpgperson
	// boardgamecompany
	// rpgcompany
	// videogamecompany

	@GET("/xmlapi2/hot")
	HotnessResponse getHotness(@Query("type") String type);

	static class HotnessResponse {
		@Attribute(name = "termsofuse")
		private String termsOfUse;

		@ElementList(name = "items", inline = true)
		public List<HotGame> games;
	}
}
