package com.boardgamegeek.ui.adapter;


import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Thread;
import com.boardgamegeek.ui.ThreadActivity;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ForumRecyclerViewAdapter extends PaginatedRecyclerViewAdapter<Thread> {
	private final int forumId;
	private final String forumTitle;
	private final int gameId;
	private final String gameName;

	public ForumRecyclerViewAdapter(Context context, PaginatedData<Thread> data, int forumId, String forumTitle, int gameId, String gameName) {
		super(context, R.layout.row_forum_thread, data);
		this.forumId = forumId;
		this.forumTitle = forumTitle;
		this.gameId = gameId;
		this.gameName = gameName;
	}

	@NonNull
	@Override
	protected PaginatedItemViewHolder getViewHolder(View itemView) {
		return new ThreadViewHolder(itemView);
	}

	public class ThreadViewHolder extends PaginatedItemViewHolder {
		private int threadId;
		@BindView(R.id.subject) TextView subjectView;
		@BindView(R.id.author) TextView authorView;
		@BindView(R.id.number_of_articles) TextView numberOfArticlesView;
		@BindView(R.id.last_post_date) TimestampView lastPostDateView;
		@BindView(R.id.post_date) TimestampView postDateView;

		public ThreadViewHolder(View view) {
			super(view);
			ButterKnife.bind(this, view);
		}

		@Override
		protected void bind(Thread item) {
			final Context context = itemView.getContext();
			threadId = item.id;
			subjectView.setText(item.subject);
			authorView.setText(context.getString(R.string.forum_thread_author, item.author));
			int replies = item.numberOfArticles - 1;
			numberOfArticlesView.setText(context.getResources().getQuantityString(R.plurals.forum_thread_replies, replies, replies));
			lastPostDateView.setTimestamp(item.lastPostDate());
			postDateView.setTimestamp(item.lastPostDate());
			itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(context, ThreadActivity.class);
					intent.putExtra(ActivityUtils.KEY_THREAD_ID, threadId);
					intent.putExtra(ActivityUtils.KEY_THREAD_SUBJECT, subjectView.getText());
					intent.putExtra(ActivityUtils.KEY_FORUM_ID, forumId);
					intent.putExtra(ActivityUtils.KEY_FORUM_TITLE, forumTitle);
					intent.putExtra(ActivityUtils.KEY_GAME_ID, gameId);
					intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
					context.startActivity(intent);
				}
			});
		}
	}
}