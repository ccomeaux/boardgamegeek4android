package com.boardgamegeek.model;

import org.simpleframework.xml.ElementList;

import java.util.List;

public class ThingResponse {
	public static final int PAGE_SIZE = 100;

	@ElementList(name = "items", inline = true)
	public List<Game> games;
}
