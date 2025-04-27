package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

/** @noinspection unused*/
@SuppressWarnings("SpellCheckingInspection" )
@Root(name = "item")
public class HotGameRemote {
	/** @noinspection unused*/
	@Attribute public int id;
	/** @noinspection unused*/
	@Attribute public int rank;
	/** @noinspection unused*/
	@Attribute(name = "value")
	@Path("thumbnail") public String thumbnailUrl;
	/** @noinspection unused*/
	@Attribute(name = "value")
	@Path("name") public String name;
	/** @noinspection unused*/
	@Attribute(name = "value")
	@Path("yearpublished") public int yearPublished;
}
