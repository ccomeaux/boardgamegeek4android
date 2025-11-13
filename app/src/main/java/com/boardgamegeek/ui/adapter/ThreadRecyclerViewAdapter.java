package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.databinding.RowThreadArticleBinding;
import com.boardgamegeek.entities.ForumEntity.ForumType;
import com.boardgamegeek.extensions.TextViewUtils;
import com.boardgamegeek.ui.ArticleActivity;
import com.boardgamegeek.ui.loader.ThreadSafeResponse;
import com.boardgamegeek.ui.model.Article;
import com.boardgamegeek.ui.widget.TimestampView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class ThreadRecyclerViewAdapter extends RecyclerView.Adapter<ThreadRecyclerViewAdapter.ArticleViewHolder> {
	private final int threadId;
	private final String threadSubject;
	private final int forumId;
	private final String forumTitle;
	private final int objectId;
	private final String objectName;
	private final ForumType objectType;
	private final List<Article> articles;
	private final LayoutInflater inflater;

	public ThreadRecyclerViewAdapter(Context context, ThreadSafeResponse thread, int forumId, String forumTitle, int objectId, String objectName, ForumType objectType) {
		threadId = thread.getThreadId();
		threadSubject = thread.getThreadSubject();
		this.forumId = forumId;
		this.forumTitle = forumTitle;
		this.objectId = objectId;
		this.objectName = objectName;
		this.objectType = objectType;
		articles = thread.getArticles();
		inflater = LayoutInflater.from(context);
		setHasStableIds(true);
	}

	@NotNull
	@Override
	public ArticleViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
		RowThreadArticleBinding binding = RowThreadArticleBinding.inflate(inflater, parent, false);
		return new ArticleViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NotNull ArticleViewHolder holder, int position) {
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
		return article.getId();
	}

	public int getPosition(int articleId) {
		if (articles == null) return RecyclerView.NO_POSITION;
		for (int i = 0; i < articles.size(); i++) {
			if (articles.get(i).getId() == articleId) return i;
		}
		return RecyclerView.NO_POSITION;
	}

	public class ArticleViewHolder extends RecyclerView.ViewHolder {
		private final RowThreadArticleBinding binding;

		public ArticleViewHolder(RowThreadArticleBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		public void bind(final Article article) {
			if (article == null) return;

			if (article.getPostTicks() > 0L) {
				binding.rowHeader.setVisibility(View.VISIBLE);
				if (TextUtils.isEmpty(article.getUsername())) {
					binding.username.setVisibility(View.GONE);
				} else {
					binding.username.setText(article.getUsername());
					binding.username.setVisibility(View.VISIBLE);
				}
				binding.postDate.setTimestamp(article.getPostTicks());
				if (article.getEditTicks() != article.getPostTicks()) {
					binding.editDate.setTimestamp(article.getEditTicks());
					binding.editDate.setVisibility(View.VISIBLE);
					binding.dateDivider.setVisibility(View.VISIBLE);
				} else {
					binding.editDate.setVisibility(View.GONE);
					binding.dateDivider.setVisibility(View.GONE);
				}
			} else {
				binding.rowHeader.setVisibility(View.GONE);
			}
			TextViewUtils.setTextMaybeHtml(binding.body, article.getBody().trim());
			binding.viewButton.setOnClickListener(v -> ArticleActivity.start(v.getContext(), threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType, article));
		}
	}
}
