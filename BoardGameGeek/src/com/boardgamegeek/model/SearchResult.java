package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(name = "item")
public class SearchResult {
	public static final int NAME_TYPE_PRIMARY = 0;
	public static final int NAME_TYPE_ALTERNATE = 1;
	public static final int NAME_TYPE_UNKNOWN = -1;

	/**
	 * Includes boardgame, boardgameexpansion, videogame, rpg, rpgitem
	 */
	@Attribute
	private String type;

	@Attribute
	public int id;

	@Path("name")
	@Attribute(name = "value")
	public String name;

	@Path("name")
	@Attribute(name = "type")
	private String nameType;

	@Path("yearpublished")
	@Attribute(name = "value")
	public int yearPublished;

	public int getNameType() {
		if ("primary".equals(nameType)) {
			return NAME_TYPE_PRIMARY;
		} else if ("alternate".equals(nameType)) {
			return NAME_TYPE_ALTERNATE;
		}
		return NAME_TYPE_UNKNOWN;
	}
}
