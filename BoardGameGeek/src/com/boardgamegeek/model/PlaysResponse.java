package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class PlaysResponse {
	@Attribute
	private String username;

	@Attribute
	private int userid;

	@Attribute
	private int total;

	@Attribute
	private int page;

	@Attribute
	private String termsofuse;

	@ElementList(inline = true, required = false)
	public List<Play> plays;
}
