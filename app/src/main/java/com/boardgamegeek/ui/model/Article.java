package com.boardgamegeek.ui.model;

import com.boardgamegeek.util.DateTimeUtils;

public class Article {
	private int id;
	private String username;
	private String link;
	private long postTicks;
	private long editTicks;
	private int numedits;
	private String body;

	public static Article fromApiModel(com.boardgamegeek.model.Article apiArticle) {
		Article article = new Article();
		article.id = apiArticle.getId();
		article.username = apiArticle.getUsername();
		article.link = apiArticle.getLink();
		article.numedits = apiArticle.getNumberOfEdits();
		final String body = apiArticle.getBody();
		if (body == null) {
			article.body = "";
		} else {
			article.body = body.trim();
		}
		article.postTicks = DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, apiArticle.postDate(), com.boardgamegeek.model.Article.FORMAT);
		article.editTicks = DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, apiArticle.editDate(), com.boardgamegeek.model.Article.FORMAT);
		return article;
	}

	public int getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getLink() {
		return link;
	}

	public String getBody() {
		return body;
	}

	public long getPostTicks() {
		return postTicks;
	}

	public long getEditTicks() {
		return editTicks;
	}

	public int getNumberOfEdits() {
		return numedits;
	}
}
