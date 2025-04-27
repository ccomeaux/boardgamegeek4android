package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
@Root(name = "article")
public class ArticleRemote {
	@Attribute public int id;
	@Attribute(required = false) public String username;
	@Attribute public String link;
	@Attribute(required = false) public String postdate;
	@Attribute(required = false) public String editdate;
	@Attribute(required = false) public int numedits;
	@Element(required = false) public String subject;
	@Element(required = false) public String body;
}
