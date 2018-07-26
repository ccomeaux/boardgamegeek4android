package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

public class PlaysResponse {
	private static final int PAGE_SIZE = 100;

	@Attribute(required = false) private String username;
	@Attribute(required = false) private int userid;
	@Attribute(required = false) private int total;
	@Attribute(required = false) private int page;
	@ElementList(inline = true, required = false) public List<Play> plays;

	public boolean hasMorePages() {
		return page * PAGE_SIZE < total;
	}
}
