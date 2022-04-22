package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@SuppressWarnings("unused")
@Root(name = "item")
public class PersonItem {
	@Attribute public String type;
	@Attribute public String id;

	@Element(required = false) public String thumbnail;

	@Element(required = false) public String image;
}
