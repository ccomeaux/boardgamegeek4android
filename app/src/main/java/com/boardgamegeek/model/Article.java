package com.boardgamegeek.model;

import com.boardgamegeek.util.DateTimeUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Root(name = "article")
public class Article {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

	private long postDateTime = DateTimeUtils.UNPARSED_DATE;
	private long editDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute
	public int id;

	@Attribute
	public String username;

	@Attribute
	public String link;

	@Attribute
	private String postdate;

	@Attribute
	private String editdate;

	@Attribute
	private int numedits;

	@Element(required = false)
	private String subject;

	@Element(required = false)
	public String body;

	public long postDate() {
		postDateTime = DateTimeUtils.tryParseDate(postDateTime, postdate, FORMAT);
		return postDateTime;
	}

	public long editDate() {
		editDateTime = DateTimeUtils.tryParseDate(editDateTime, editdate, FORMAT);
		return editDateTime;
	}

	public int getNumberOfEdits() {
		return numedits;
	}
}
