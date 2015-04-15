package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class ForumListResponse {
	@Attribute
	public String type;

	@Attribute
	public int id;

	@Attribute(name = "termsofuse")
	private String termsOfUse;

	@ElementList(inline = true)
	public List<Forum> forums;
}
