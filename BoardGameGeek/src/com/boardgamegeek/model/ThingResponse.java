package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class ThingResponse {
	public static final int PAGE_SIZE = 100;

	@Attribute
	private String termsofuse;

	@ElementList(name = "items", inline = true)
	public List<Game> games;
}
