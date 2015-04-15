package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;

public class Company {
	@Attribute(name = "termsofuse")
	private String termsOfUse;

	public int id;

	@Element
	@Path("company")
	public String name;

	@Element(required = false)
	@Path("company")
	public String description;
}
