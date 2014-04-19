package com.boardgamegeek.model;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import com.boardgamegeek.util.DateTimeUtils;

@Root(name = "article")
public class Article {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

	private long mEditDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute
	private int id;

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

	@Element
	private String subject;

	@Element
	public String body;

	public long editDate() {
		mEditDateTime = DateTimeUtils.tryParseDate(mEditDateTime, editdate, FORMAT);
		return mEditDateTime;
	}
}
