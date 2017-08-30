package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.ArticleActivity;
import com.boardgamegeek.ui.loader.ThreadSafeResponse;
import com.boardgamegeek.ui.model.Article;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class ThreadRecyclerViewAdapter extends RecyclerView.Adapter<ThreadRecyclerViewAdapter.ArticleViewHolder> {
	private final int threadId;
	private final String threadSubject;
	private final int forumId;
	private final String forumTitle;
	private final int gameId;
	private final String gameName;
	private final List<Article> articles;
	private final LayoutInflater inflater;

	public ThreadRecyclerViewAdapter(Context context, ThreadSafeResponse thread, int forumId, String forumTitle, int gameId, String gameName) {
		threadId = thread.getThreadId();
		threadSubject = thread.getThreadSubject();
		this.forumId = forumId;
		this.forumTitle = forumTitle;
		this.gameId = gameId;
		this.gameName = gameName;
		articles = thread.getArticles();
		inflater = LayoutInflater.from(context);
		setHasStableIds(true);
	}

	@Override
	public ArticleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new ArticleViewHolder(inflater.inflate(R.layout.row_thread_article, parent, false));
	}

	@Override
	public void onBindViewHolder(ArticleViewHolder holder, int position) {
		holder.bind(articles.get(position));
	}

	@Override
	public int getItemCount() {
		return articles == null ? 0 : articles.size();
	}

	@Override
	public long getItemId(int position) {
		Article article = articles.get(position);
		if (article == null) return RecyclerView.NO_ID;
		return (long) article.id();
	}

	public int getPosition(int articleId) {
		if (articles == null) return RecyclerView.NO_POSITION;
		for (int i = 0; i < articles.size(); i++) {
			if (articles.get(i).id() == articleId) return i;
		}
		return RecyclerView.NO_POSITION;
	}

	public class ArticleViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.username) TextView usernameView;
		@BindView(R.id.post_date) TimestampView postDateView;
		@BindView(R.id.date_divider) View dateDivider;
		@BindView(R.id.edit_date) TimestampView editDateView;
		@BindView(R.id.body) TextView bodyView;
		@BindView(R.id.view_button) View viewButton;

		@DebugLog
		public ArticleViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		public void bind(final Article article) {
			if (article == null) return;

			usernameView.setText(article.username());
			postDateView.setTimestamp(article.postTicks());
			if (article.editTicks() != article.postTicks()) {
				editDateView.setTimestamp(article.editTicks());
				editDateView.setVisibility(View.VISIBLE);
				dateDivider.setVisibility(View.VISIBLE);
			} else {
				editDateView.setVisibility(View.GONE);
				dateDivider.setVisibility(View.GONE);
			}
			if (TextUtils.isEmpty(article.body())) {
				bodyView.setText("");
			} else {
				UIUtils.setTextMaybeHtml(bodyView, article.body().trim());
			}
			itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ArticleActivity.start(v.getContext(), threadId, threadSubject, forumId, forumTitle, gameId, gameName, article);
				}
			});
		}
	}
}
