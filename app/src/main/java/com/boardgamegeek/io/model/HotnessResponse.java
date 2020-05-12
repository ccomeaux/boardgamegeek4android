package com.boardgamegeek.io.model;

import org.simpleframework.xml.ElementList;

import java.util.List;

public class HotnessResponse {
	@ElementList(name = "items", inline = true)
	public List<HotGame> games;
}
