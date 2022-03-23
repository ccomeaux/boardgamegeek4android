package com.boardgamegeek.io.model;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
public class GeekListEntry {
	public String href;
	public int numpositive;
	public String username;
	public int numitems;
	public String title;
	private String postdate; // always null
	private String lastreplydate; // always null
	private String pagination; // not useful
}
