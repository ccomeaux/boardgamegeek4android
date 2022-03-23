package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@SuppressWarnings("unused")
@Root(name = "buddies")
public class Buddies {
	@Attribute(name = "total") private int buddyTotal;
	@Attribute(name = "page") private int buddyPage;
	@ElementList(inline = true, required = false) public List<Buddy> buddies;
}
