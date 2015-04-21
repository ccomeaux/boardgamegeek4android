package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.List;

public class ThreadResponse {
	@Attribute
	private int id;

	@Attribute(name = "numarticles")
	private int numberOfArticles;

	@Attribute
	private String link;

	@Element
	private String subject;

	@ElementList
	public List<Article> articles;
}
