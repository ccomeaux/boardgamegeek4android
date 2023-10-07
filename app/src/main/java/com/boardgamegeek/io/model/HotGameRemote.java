package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@SuppressWarnings("SpellCheckingInspection" )
@Root(name = "item")
public class HotGameRemote {
	@Attribute public int id;
	@Attribute public int rank;

	@Attribute(name = "value")
	@Path("thumbnail") public String thumbnailUrl;

	@Attribute(name = "value")
	@Path("name") public String name;

	@Attribute(name = "value")
	@Path("yearpublished") public int yearPublished;
}
