package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "buddy")
public class Buddy {
	@Attribute
	public int id;

	@Attribute
	public String name;

	public static Buddy fromUser(User user) {
		Buddy buddy = new Buddy();
		buddy.id = user.id;
		buddy.name = user.name;
		return buddy;
	}
}
