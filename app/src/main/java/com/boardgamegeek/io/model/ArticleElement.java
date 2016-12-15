package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.Locale;

@SuppressWarnings("unused")
@Root(name = "article")
public class ArticleElement {
	public static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

	@Attribute public int id;
	@Attribute public String username;
	@Attribute public String link;
	@Attribute public String postdate;
	@Attribute public String editdate;
	@Attribute public int numedits;
	@Element(required = false) public String subject;
	@Element(required = false) public String body;
}
