package com.boardgamegeek.io.model;

import org.simpleframework.xml.ElementList;

import java.util.List;

public class ThingResponse {
	@ElementList(name = "items", inline = true, required = false)
	public List<Game> games;
}
