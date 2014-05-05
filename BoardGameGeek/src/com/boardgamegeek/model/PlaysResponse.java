package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class PlaysResponse {
	private static final int PAGE_SIZE = 100;

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

	public boolean hasMorePages() {
		return page * PAGE_SIZE < total;
	}

	public long getNewestDate() {
		long date = 0;
		for (Play play : plays) {
			if (play.getDateInMillis() > date) {
				date = play.getDateInMillis();
			}
		}
		return date;
	}

	public long getOldestDate() {
		long date = Long.MAX_VALUE;
		for (Play play : plays) {
			if (play.getDateInMillis() < date) {
				date = play.getDateInMillis();
			}
		}
		return date;
	}
}
