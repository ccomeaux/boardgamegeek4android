package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

public class CollectionResponse {
	@Attribute(required = false)
	public int totalitems;

	@Attribute(required = false)
	private String pubdate;

	@ElementList(inline = true, required = false)
	public List<CollectionItem> items;
}