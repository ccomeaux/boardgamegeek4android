package com.boardgamegeek.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;

public class Person {
	public int id;

	@Element
	@Path("person")
	public String name;

	@Element(required = false)
	@Path("person")
	public String description;
}
