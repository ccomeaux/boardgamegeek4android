package com.boardgamegeek.io.model;

import com.boardgamegeek.io.model.Game.Rank;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.List;

@Root(name = "item")
public class CollectionItem {
	@Attribute private String objecttype; // "thing"
	@Attribute public int objectid;
	@Attribute private String subtype; // boardgame, boardgameexpansion, videogame, rpg, rpgitem
	@Attribute public String collid;
	@Path("name") @Text public String name;
	@Path("name") @Attribute public int sortindex;

	// non-brief
	@Element(required = false) public String originalname;
	@Element(required = false) public String yearpublished;
	@Element(required = false) public String image;
	@Element(required = false) public String thumbnail;

	// stats
	@Element(required = false) public Statistics stats;

	@Path("status") @Attribute public String own;
	@Path("status") @Attribute public String prevowned;
	@Path("status") @Attribute public String fortrade;
	@Path("status") @Attribute public String want;
	@Path("status") @Attribute public String wanttoplay;
	@Path("status") @Attribute public String wanttobuy;
	@Path("status") @Attribute public String wishlist;
	@Path("status") @Attribute(required = false) public int wishlistpriority;
	@Path("status") @Attribute public String preordered;
	@Path("status") @Attribute public String lastmodified;

	// non-brief
	@Element(required = false) public int numplays;

	// show private
	@Path("privateinfo") @Attribute(required = false) public String pp_currency;
	@Path("privateinfo") @Attribute(required = false, empty = "0.0") public String pricepaid;
	@Path("privateinfo") @Attribute(required = false) public String cv_currency;
	@Path("privateinfo") @Attribute(required = false, empty = "0.0") public String currvalue;
	@Path("privateinfo") @Attribute(required = false) public String quantity;
	@Path("privateinfo") @Attribute(required = false) public String acquisitiondate; // "2000-12-08"
	@Path("privateinfo") @Attribute(required = false) public String acquiredfrom;
	@Path("privateinfo") @Element(required = false) public String privatecomment;

	// non-brief
	@Element(required = false) public String comment;
	@Element(required = false) public String conditiontext;
	@Element(required = false) public String wantpartslist;
	@Element(required = false) public String haspartslist;
	@Element(required = false) public String wishlistcomment;

	public static class Statistics {
		@Attribute(required = false) public int minplayers;
		@Attribute(required = false) public int maxplayers;
		@Attribute(required = false) public int minplaytime;
		@Attribute(required = false) public int maxplaytime;
		@Attribute(required = false) public int playingtime;
		@Attribute public String numowned;
		@Path("rating") @Attribute(name = "value") public String rating;
		@Path("rating/usersrated") @Attribute(name = "value", required = false) public String usersrated; // non-brief
		@Path("rating/average") @Attribute(name = "value") public String average;
		@Path("rating/bayesaverage") @Attribute(name = "value") public String bayesaverage;
		@Path("rating/stddev") @Attribute(name = "value", required = false) public String stddev; // non-brief
		@Path("rating/median") @Attribute(name = "value", required = false) public String median; // non-brief
		@Path("rating") @ElementList(required = false) public List<Rank> ranks; // non-brief
	}

}
