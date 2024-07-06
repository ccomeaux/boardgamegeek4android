package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

@SuppressWarnings("unused")
public class ForumListResponse {
	@Attribute public String type;
	@Attribute public int id;
	@ElementList(inline = true) public List<ForumRemote> forums;
}
