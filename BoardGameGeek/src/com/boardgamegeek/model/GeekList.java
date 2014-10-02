package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name = "geeklist")
public class GeekList {
	@Attribute public int id;

	@Attribute public String termsofuse;

	@Element public String postdate;

	@Element(name = "postdate_timestamp") public String postdateTimestamp;

	@Element public String editdate;

	@Element(name = "editdate_timestamp") public String editdateTimestamp;

	@Element public String thumbs;

	@Element public String numitems;

	@Element public String username;

	@Element public String title;

	@Element public String description;

	@ElementList(name = "comment", inline = true, required = false) public List<GeekListComment> comments;

	@ElementList(name = "item", inline = true, required = false) public List<GeekListItem> items;
}
