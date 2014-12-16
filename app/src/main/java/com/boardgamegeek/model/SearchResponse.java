package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class SearchResponse {
	@Attribute
	public int total;

	@Attribute(name = "termsofuse")
	private String termsOfUse;

	@ElementList(name = "items", inline = true, required = false)
	public List<SearchResult> games;
}
