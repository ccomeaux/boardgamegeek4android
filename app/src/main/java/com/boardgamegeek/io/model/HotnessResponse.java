package com.boardgamegeek.io.model;

import org.simpleframework.xml.ElementList;

import java.util.List;

/** @noinspection unused*/
public class HotnessResponse {
	@ElementList(name = "items", inline = true)
	public List<HotGameRemote> games;
}
