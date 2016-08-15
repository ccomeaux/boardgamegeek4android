package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.ui.ForumActivity;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ForumsRecyclerViewAdapter extends RecyclerView.Adapter<ForumsRecyclerViewAdapter.ForumViewHolder> {
	public static final int ITEM_VIEW_TYPE_FORUM = 0;
	public static final int ITEM_VIEW_TYPE_HEADER = 1;

	private final List<Forum> forums;
	private final LayoutInflater inflater;
	private final Resources resources;
	private final int gameId;
	private final String gameName;

	public ForumsRecyclerViewAdapter(Context context, List<Forum> forums, int gameId, String gameName) {
		this.forums = forums;
		this.gameId = gameId;
		this.gameName = gameName;
		inflater = LayoutInflater.from(context);
		resources = context.getResources();
		setHasStableIds(true);
	}

	@Override
	public ForumViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		switch (viewType) {
			case ITEM_VIEW_TYPE_HEADER:
				return new HeaderViewHolder(inflater.inflate(R.layout.row_header, parent, false));
			case ITEM_VIEW_TYPE_FORUM:
				return new ForumItemViewHolder(inflater.inflate(R.layout.row_forum, parent, false));
		}
		return null;
	}

	@Override
	public void onBindViewHolder(ForumViewHolder holder, int position) {
		switch (holder.getItemViewType()) {
			case ITEM_VIEW_TYPE_HEADER:
				((HeaderViewHolder) holder).bind(forums.get(position));
				break;
			case ITEM_VIEW_TYPE_FORUM:
				((ForumItemViewHolder) holder).bind(forums.get(position));
				break;
		}
	}

	@Override
	public int getItemCount() {
		return forums == null ? 0 : forums.size();
	}

	@Override
	public int getItemViewType(int position) {
		try {
			Forum forum = forums.get(position);
			if (forum != null && forum.isHeader()) {
				return ITEM_VIEW_TYPE_HEADER;
			}
			return ITEM_VIEW_TYPE_FORUM;
		} catch (ArrayIndexOutOfBoundsException e) {
			return ITEM_VIEW_TYPE_FORUM;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public class ForumViewHolder extends RecyclerView.ViewHolder {
		public ForumViewHolder(View itemView) {
			super(itemView);
		}
	}

	public class ForumItemViewHolder extends ForumViewHolder {
		@BindView(R.id.forum_title) TextView forumTitleView;
		@BindView(R.id.number_of_threads) TextView numberOfThreadsView;
		@BindView(R.id.last_post_date) TimestampView lastPostDateView;

		public ForumItemViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		public void bind(final Forum forum) {
			if (forum == null) return;

			final Context context = itemView.getContext();
			forumTitleView.setText(forum.title);
			numberOfThreadsView.setText(resources.getQuantityString(R.plurals.forum_threads, forum.numberOfThreads, forum.numberOfThreads));
			lastPostDateView.setTimestamp(forum.lastPostDate());

			itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(context, ForumActivity.class);
					intent.putExtra(ActivityUtils.KEY_FORUM_ID, forum.id);
					intent.putExtra(ActivityUtils.KEY_FORUM_TITLE, forum.title);
					intent.putExtra(ActivityUtils.KEY_GAME_ID, gameId);
					intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
					context.startActivity(intent);
				}
			});
		}
	}

	public class HeaderViewHolder extends ForumViewHolder {
		@BindView(android.R.id.title) TextView header;

		public HeaderViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		public void bind(Forum forum) {
			if (forum != null) {
				header.setText(forum.title);
			}
		}
	}
}
