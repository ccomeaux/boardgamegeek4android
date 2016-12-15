package com.boardgamegeek.model;

import com.boardgamegeek.util.DateTimeUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Root(name = "thread")
public class Thread {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	private long postDateTime = DateTimeUtils.UNPARSED_DATE;
	private long lastPostDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute
	public int id;

	@Attribute
	public String subject;

	@Attribute
	public String author;

	@Attribute(name = "numarticles", required = false)
	public int numberOfArticles;

	@Attribute
	private String postdate;

	@Attribute
	private String lastpostdate;

	public long postDate() {
		postDateTime = DateTimeUtils.tryParseDate(postDateTime, postdate, FORMAT);
		return postDateTime;
	}

	public long lastPostDate() {
		lastPostDateTime = DateTimeUtils.tryParseDate(lastPostDateTime, lastpostdate, FORMAT);
		return lastPostDateTime;
	}
}