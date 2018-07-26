package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.List;

@SuppressWarnings("unused")
public class ThreadResponse {
	@Attribute(required = false) public int id;
	@Attribute(required = false) private int numarticles;
	@Attribute(required = false) private String link;
	@Element(required = false) public String subject;
	@ElementList(required = false) public List<ArticleElement> articles;
}
