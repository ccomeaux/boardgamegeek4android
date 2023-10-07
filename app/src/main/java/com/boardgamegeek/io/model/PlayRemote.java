package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.List;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
@Root(name = "play")
public class PlayRemote {
	@Attribute public int id;
	@Attribute public String date;
	@Attribute public int quantity;
	@Attribute public int length;
	@Attribute public int incomplete;
	@Attribute public int nowinstats;
	@Attribute public String location;
	@Path("item") @Attribute public String name;
	@Path("item") @Attribute public int objectid;
	@Path("item") @Attribute public String objecttype;
	@Element(required = false) public String comments;
	@ElementList(required = false) public List<PlayerRemote> players;
	@Path("item") @ElementList public List<PlayRemote.Subtype> subtypes;
	@Root(name = "subtype")
	public static class Subtype {
		@Attribute public String value;
	}
}
