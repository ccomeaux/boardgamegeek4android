package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "item")
public class GeeklistItem {
	@Attribute
    public String editdate;

	@Attribute
    public String id;

	@Attribute
    public String imageid;

	@Attribute
	public String objectid;

	@Attribute
	public String objectname;

	@Attribute
	public String objecttype;

	@Attribute
	public String postdate;

	@Attribute
	public String subtype;

	@Attribute
	public String thumbs;

	@Attribute
	public String username;

	@Element
    public String body;

	@ElementList(name = "comment", inline = true, required = false)
    public List<Comment> comments;
}
