package com.boardgamegeek.ui.loader;


import com.boardgamegeek.io.model.ArticleElement;
import com.boardgamegeek.io.model.ThreadResponse;
import com.boardgamegeek.ui.model.Article;
import com.boardgamegeek.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class ThreadSafeResponse extends SafeResponse<ThreadResponse> {
	private List<Article> articles;

	public ThreadSafeResponse(Call<ThreadResponse> call) {
		super(call);
	}

	@Override
	protected void mapBody(ThreadResponse body) {
		super.mapBody(body);
		if (body == null || body.articles == null) {
			articles = new ArrayList<>(0);
		} else {
			articles = new ArrayList<>(body.articles.size());
			for (ArticleElement articleElement : body.articles) {
				articles.add(Article.builder()
					.setId(articleElement.id)
					.setUsername(articleElement.username)
					.setLink(articleElement.link)
					.setPostTicks(DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, articleElement.postdate, ArticleElement.FORMAT))
					.setEditTicks(DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, articleElement.editdate, ArticleElement.FORMAT))
					.setBody(articleElement.body == null ? "" : articleElement.body.trim())
					.setNumberOfEdits(articleElement.numedits)
					.build());
			}
		}
	}

	public List<Article> getArticles() {
		return articles;
	}
}
