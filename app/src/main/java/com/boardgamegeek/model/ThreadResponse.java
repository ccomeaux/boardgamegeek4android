package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public class ThreadResponse {
	@Attribute
	private int id;

	@Attribute(name = "numarticles")
	private int numberOfArticles;

	@Attribute
	private String link;

	@Attribute(name = "termsofuse")
	private String termsOfUse;

	@Element
	private String subject;

	@ElementList
	public List<Article> articles;
}
