package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

@SuppressWarnings("unused")
public class SearchResponse {
	@Attribute public int total;
	@ElementList(inline = true, required = false) public List<SearchResult> items;
}
