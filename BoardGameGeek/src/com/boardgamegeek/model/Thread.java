package com.boardgamegeek.model;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import com.boardgamegeek.util.DateTimeUtils;

@Root(name = "thread")
public class Thread {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	private long mPostDateTime = DateTimeUtils.UNPARSED_DATE;
	private long mLastPostDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute
	public int id;

	@Attribute
	public String subject;

	@Attribute
	public String author;

	@Attribute(name = "numarticles")
	public int numberOfArticles;

	@Attribute
	private String postdate;

	@Attribute
	private String lastpostdate;

	public long postDate() {
		mPostDateTime = DateTimeUtils.tryParseDate(mPostDateTime, postdate, FORMAT);
		return mPostDateTime;
	}

	public long lastPostDate() {
		mLastPostDateTime = DateTimeUtils.tryParseDate(mLastPostDateTime, lastpostdate, FORMAT);
		return mLastPostDateTime;
	}
}