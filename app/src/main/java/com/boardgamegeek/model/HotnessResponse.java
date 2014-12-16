package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class HotnessResponse {
	@Attribute(name = "termsofuse")
	private String termsOfUse;

	@ElementList(name = "items", inline = true)
	public List<HotGame> games;
}
