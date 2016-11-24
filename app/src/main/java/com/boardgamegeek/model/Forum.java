package com.boardgamegeek.model;

import com.boardgamegeek.util.DateTimeUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Root(name = "forum")
public class Forum {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	private long lastPostDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute
	public int id;

	@Attribute
	private int groupid;

	@Attribute
	public String title;

	@Attribute
	private int noposting;

	@Attribute
	private String description;

	@Attribute(name = "numthreads")
	public int numberOfThreads;

	@Attribute
	private int numposts;

	@Attribute
	private String lastpostdate;

	public boolean isHeader() {
		return noposting == 1;
	}

	public long lastPostDate() {
		lastPostDateTime = DateTimeUtils.tryParseDate(lastPostDateTime, lastpostdate, FORMAT);
		return lastPostDateTime;
	}

	@Override
	public String toString() {
		return "" + id + ": " + title + " - " + description;
	}
}