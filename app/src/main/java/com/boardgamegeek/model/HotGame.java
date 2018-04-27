package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(name = "item")
public class HotGame {
	@Attribute private int id;

	@Attribute private int rank;

	@Attribute(name = "value")
	@Path("thumbnail") private String thumbnailUrl;

	@Attribute(name = "value")
	@Path("name") private String name;

	@Attribute(name = "value")
	@Path("yearpublished") private int yearPublished;

	public int getId() {
		return id;
	}

	public int getRank() {
		return rank;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl == null ? "" : thumbnailUrl;
	}

	public String getName() {
		return name;
	}

	public int getYearPublished() {
		return yearPublished;
	}
}
