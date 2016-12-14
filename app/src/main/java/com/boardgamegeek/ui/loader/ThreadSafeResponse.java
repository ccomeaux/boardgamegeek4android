package com.boardgamegeek.ui.loader;


import com.boardgamegeek.io.model.Article;
import com.boardgamegeek.io.model.ThreadResponse;
import com.boardgamegeek.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class ThreadSafeResponse extends SafeResponse<ThreadResponse> {
	public ThreadSafeResponse(Call<ThreadResponse> call) {
		super(call);
	}

	public List<com.boardgamegeek.ui.model.Article> getArticles() {
		List<Article> apiArticles = body == null ? null : body.articles;
		if (apiArticles == null) return new ArrayList<>(0);
		final List<com.boardgamegeek.ui.model.Article> articles = new ArrayList<>(apiArticles.size());
		for (Article apiArticle : apiArticles) {
			articles.add(com.boardgamegeek.ui.model.Article.builder()
				.setId(apiArticle.id)
				.setUsername(apiArticle.username)
				.setLink(apiArticle.link)
				.setPostTicks(DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, apiArticle.postdate, Article.FORMAT))
				.setEditTicks(DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, apiArticle.editdate, Article.FORMAT))
				.setBody(apiArticle.body == null ? "" : apiArticle.body.trim())
				.setNumberOfEdits(apiArticle.numedits)
				.build());
		}
		return articles;
	}
}
