package com.boardgamegeek.ui.loader;


import com.boardgamegeek.io.model.ArticleElement;
import com.boardgamegeek.io.model.ThreadResponse;
import com.boardgamegeek.ui.model.Article;
import com.boardgamegeek.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class ThreadSafeResponse extends SafeResponse<ThreadResponse> {
	private int threadId;
	private String threadSubject;
	private List<Article> articles;

	public ThreadSafeResponse(Call<ThreadResponse> call) {
		super(call);
	}

	@Override
	protected void mapBody(ThreadResponse body) {
		super.mapBody(body);
		threadId = body.id;
		threadSubject = body.subject;
		if (body.articles == null) {
			articles = new ArrayList<>(0);
		} else {
			articles = new ArrayList<>(body.articles.size());
			for (ArticleElement articleElement : body.articles) {
				articles.add(new Article(
					articleElement.id,
					articleElement.username,
					articleElement.link,
					DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, articleElement.postdate, ArticleElement.FORMAT),
					DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, articleElement.editdate, ArticleElement.FORMAT),
					articleElement.body == null ? "" : articleElement.body.trim(),
					articleElement.numedits
				));
			}
		}
	}

	public int getThreadId() {
		return threadId;
	}

	public String getThreadSubject() {
		return threadSubject;
	}

	public List<Article> getArticles() {
		return articles;
	}
}
