package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.List;

public class ThreadResponse {
	@Attribute(required = false) private int id;
	@Attribute(name = "numarticles", required = false) private int numberOfArticles;
	@Attribute(required = false) private String link;
	@Element(required = false) private String subject;
	@ElementList(required = false) private List<Article> articles;

	public List<Article> getArticles() {
		if (articles == null) {
			return new ArrayList<>();
		}
		return articles;
	}
}
