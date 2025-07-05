package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
@Root(name = "item")
public class SearchResultRemote {
	/**
	 * Includes boardgame, boardgameexpansion, videogame, rpg, rpgitem
	 */
	@Attribute private String type;
	@Attribute public int id;
	@Path("name") @Attribute(name = "value") public String name;
	@Path("name") @Attribute(name = "type") public String nameType;
	@Path("yearpublished") @Attribute(name = "value") public String yearPublished;
	public static final String NAME_TYPE_PRIMARY = "primary";
	public static final String NAME_TYPE_ALTERNATE = "alternate";
}
