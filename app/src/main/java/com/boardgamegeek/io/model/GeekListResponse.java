package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "geeklist")
public class GeekListResponse {
	@Attribute public int id;
	@Element public String postdate;
	@Element public String editdate;
	@Element(name = "postdate_timestamp") private String postdateTimestamp;
	@Element(name = "editdate_timestamp") private String editdateTimestamp;
	@Element public String thumbs;
	@Element public String numitems;
	@Element public String username;
	@Element public String title;
	@Element public String description;
	@ElementList(name = "comment", inline = true, required = false)
	public List<GeekListComment> comments;
	@ElementList(name = "item", inline = true, required = false) public List<GeekListItem> items;
}
