package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.model.Article;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class ThreadRecyclerViewAdapter extends RecyclerView.Adapter<ThreadRecyclerViewAdapter.ArticleViewHolder> {
	private final List<Article> articles;
	private final LayoutInflater inflater;

	public ThreadRecyclerViewAdapter(Context context, List<Article> articles) {
		this.articles = articles;
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
		return (long) article.getId();
	}

	public int getPosition(int articleId) {
		if (articles == null) return RecyclerView.NO_POSITION;
		for (int i = 0; i < articles.size(); i++) {
			if (articles.get(i).getId() == articleId) return i;
		}
		return RecyclerView.NO_POSITION;
	}

	public class ArticleViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.username) TextView usernameView;
		@BindView(R.id.edit_date) TimestampView editDateView;
		@BindView(R.id.body) TextView bodyView;
		@BindView(R.id.view_button) View viewButton;

		@DebugLog
		public ArticleViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		public void bind(Article article) {
			if (article == null) return;

			usernameView.setText(article.getUsername());
			editDateView.setTimestamp(article.getEditTicks());
			UIUtils.setTextMaybeHtml(bodyView, article.getBody());
			Bundle bundle = new Bundle();
			bundle.putString(ActivityUtils.KEY_USER, article.getUsername());
			bundle.putLong(ActivityUtils.KEY_POST_DATE, article.getPostTicks());
			bundle.putLong(ActivityUtils.KEY_EDIT_DATE, article.getEditTicks());
			bundle.putInt(ActivityUtils.KEY_EDIT_COUNT, article.getNumberOfEdits());
			bundle.putString(ActivityUtils.KEY_BODY, article.getBody());
			bundle.putString(ActivityUtils.KEY_LINK, article.getLink());
			bundle.putInt(ActivityUtils.KEY_ARTICLE_ID, article.getId());
			viewButton.setTag(bundle);
		}
	}
}
