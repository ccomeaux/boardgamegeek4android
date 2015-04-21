package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class CollectionResponse {
	@Attribute
	public int totalitems;

	@Attribute(required = false)
	private String pubdate;

	@ElementList(inline = true, required = false)
	public List<CollectionItem> items;
}