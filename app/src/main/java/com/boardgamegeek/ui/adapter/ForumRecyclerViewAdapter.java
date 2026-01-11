package com.boardgamegeek.ui.adapter;


import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.databinding.RowForumThreadBinding;
import com.boardgamegeek.entities.ForumEntity.ForumType;
import com.boardgamegeek.model.Thread;
import com.boardgamegeek.ui.ThreadActivity;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.ui.widget.TimestampView;

import java.text.NumberFormat;

import androidx.annotation.NonNull;

public class ForumRecyclerViewAdapter extends PaginatedRecyclerViewAdapter<Thread> {
	private final int forumId;
	private final String forumTitle;
	private final int objectId;
	private final String objectName;
	private final ForumType objectType;
	private final NumberFormat numberFormat;

	public ForumRecyclerViewAdapter(Context context, PaginatedData<Thread> data, int forumId, String forumTitle, int objectId, String objectName, ForumType objectType) {
		super(context, R.layout.row_forum_thread, data);
		this.forumId = forumId;
		this.forumTitle = forumTitle;
		this.objectId = objectId;
		this.objectName = objectName;
		this.objectType = objectType;
		numberFormat = NumberFormat.getNumberInstance();
	}

	@NonNull
	@Override
	protected PaginatedItemViewHolder getViewHolder(View itemView) {
		return new ThreadViewHolder(itemView);
	}

	public class ThreadViewHolder extends PaginatedItemViewHolder {
		private final RowForumThreadBinding binding;

		public ThreadViewHolder(View view) {
			super(view);
			binding = RowForumThreadBinding.bind(view);
		}

		@Override
		protected void bind(final Thread thread) {
			final Context context = itemView.getContext();
			binding.subject.setText(thread.subject.trim());
			binding.author.setText(thread.author);
			int replies = thread.numberOfArticles - 1;
			binding.numberOfArticles.setText(numberFormat.format(replies));
			binding.lastPostDate.setTimestamp(thread.lastPostDate());
			itemView.setOnClickListener(v -> ThreadActivity.start(context, thread.id, thread.subject, forumId, forumTitle, objectId, objectName, objectType));
		}
	}
}
