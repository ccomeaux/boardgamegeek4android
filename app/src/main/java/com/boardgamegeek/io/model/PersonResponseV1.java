package com.boardgamegeek.io.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;

public class PersonResponseV1 {
	@Element
	@Path("person")
	public String name;

	@Element(required = false)
	@Path("person")
	public String description;
}
