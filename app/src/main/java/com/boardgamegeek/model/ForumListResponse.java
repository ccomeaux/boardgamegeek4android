package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

public class ForumListResponse {
	@Attribute
	public String type;

	@Attribute
	public int id;

	@ElementList(inline = true)
	public List<Forum> forums;
}
