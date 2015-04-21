package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

public class SearchResponse {
	@Attribute
	public int total;

	@ElementList(name = "items", inline = true, required = false)
	public List<SearchResult> games;
}
