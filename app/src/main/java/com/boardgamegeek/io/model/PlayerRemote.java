package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
@Root(name = "player")
public class PlayerRemote {
	@Attribute public String username;
	@Attribute public int userid;
	@Attribute public String name;
	@Attribute public String startposition;
	@Attribute public String color;
	@Attribute public String score;
	@Attribute(name = "new") public int new_;
	@Attribute public double rating;
	@Attribute public int win;
}
