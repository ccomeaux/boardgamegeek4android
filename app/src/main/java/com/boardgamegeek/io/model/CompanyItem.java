package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(name = "item")
public class CompanyItem {
	@Attribute public String type;
	@Attribute public String id;

	@Element(required = false) public String thumbnail;
	@Element(required = false) public String image;
	@Path("name") @Attribute(name = "type") public String nameType;
	@Path("name") @Attribute public int sortindex;
	@Path("name") @Attribute(name = "value") public String name;
	@Element(required = false) public String description;
}
