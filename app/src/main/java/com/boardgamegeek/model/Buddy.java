package com.boardgamegeek.model;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "buddy")
public class Buddy {
	@Attribute
	private String id;

	@Attribute
	public String name;

	public int getId() {
		return StringUtils.parseInt(id, BggContract.INVALID_ID);
	}

	public static Buddy fromUser(User user) {
		Buddy buddy = new Buddy();
		buddy.id = String.valueOf(user.getId());
		buddy.name = user.name;
		return buddy;
	}
}
