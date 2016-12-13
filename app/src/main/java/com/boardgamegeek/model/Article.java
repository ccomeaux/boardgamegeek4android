package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Root(name = "article")
public class Article {
	public static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

	@Attribute private int id;
	@Attribute private String username;
	@Attribute private String link;
	@Attribute private String postdate;
	@Attribute private String editdate;
	@Attribute private int numedits;
	@Element(required = false) private String subject;
	@Element(required = false) private String body;

	public String getUsername() {
		return username;
	}

	public String getLink() {
		return link;
	}

	public int getId() {
		return id;
	}

	public String getBody() {
		if (body == null) return "";
		return body.trim();
	}

	public String postDate() {
		return postdate;
	}

	public String editDate() {
		return editdate;
	}

	public int getNumberOfEdits() {
		return numedits;
	}
}
