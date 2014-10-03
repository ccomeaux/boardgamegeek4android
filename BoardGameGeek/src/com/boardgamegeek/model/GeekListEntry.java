package com.boardgamegeek.model;

public class GeekListEntry {
	private String href;
	private int numpositive;
	private String username;
	private int numitems;
	private String title;
	private String postdate;
	private String lastreplydate;
	private String pagination;

	public String getTitle() {
		return title;
	}

	public int getId() {
		int start = href.indexOf("/geeklist/");
		return Integer.valueOf(href.substring(start + 10, href.lastIndexOf("/")));
	}

	public String getAuthor() {
		return username;
	}

	public int getNumberOfThumbs() {
		return numpositive;
	}

	@Override
	public String toString() {
		return getId() + ": " + title;
	}
}
