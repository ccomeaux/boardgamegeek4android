package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

public class PlaysResponse {
	private static final int PAGE_SIZE = 100;

	@Attribute(required = false)
	private String username;

	@Attribute
	private int userid;

	@Attribute
	private int total;

	@Attribute
	private int page;

	@ElementList(inline = true, required = false)
	public List<Play> plays;

	public boolean hasMorePages() {
		return page * PAGE_SIZE < total;
	}

	public long getNewestDate() {
		long date = 0;
		if (plays != null) {
			for (Play play : plays) {
				if (play.getDateInMillis() > date) {
					date = play.getDateInMillis();
				}
			}
		}
		return date;
	}

	public long getOldestDate() {
		long date = Long.MAX_VALUE;
		if (plays != null) {
			for (Play play : plays) {
				if (play.getDateInMillis() < date) {
					date = play.getDateInMillis();
				}
			}
		}
		return date;
	}
}
