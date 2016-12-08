package com.boardgamegeek.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeekListComment;
import com.boardgamegeek.ui.widget.TimestampView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GeekListCommentsRecyclerViewAdapter extends RecyclerView.Adapter<GeekListCommentsRecyclerViewAdapter.CommentViewHolder> {
	private final List<GeekListComment> comments;
	private final LayoutInflater inflater;

	public GeekListCommentsRecyclerViewAdapter(Context context, List<GeekListComment> comments) {
		this.comments = comments;
		inflater = LayoutInflater.from(context);
	}

	@Override
	public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new CommentViewHolder(inflater.inflate(R.layout.row_geeklist_comment, parent, false));
	}

	@Override
	public void onBindViewHolder(CommentViewHolder holder, int position) {
		holder.bind(comments.get(position));
	}

	@Override
	public int getItemCount() {
		return comments == null ? 0 : comments.size();
	}

	public class CommentViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.username) TextView usernameView;
		@BindView(R.id.number_of_thumbs) TextView numberOfThumbsView;
		@BindView(R.id.posted_date) TimestampView postedDateView;
		@BindView(R.id.datetime_divider) View datetimeDividerView;
		@BindView(R.id.edited_date) TimestampView editedDateView;
		@BindView(R.id.comment) TextView commentView;

		public CommentViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		public void bind(final GeekListComment comment) {
			usernameView.setText(comment.getUsername());
			numberOfThumbsView.setText(String.valueOf(comment.getNumberOfThumbs()));
			postedDateView.setTimestamp(comment.getPostDate());
			if (comment.getEditDate() == comment.getPostDate()) {
				editedDateView.setVisibility(View.GONE);
				datetimeDividerView.setVisibility(View.GONE);
			} else {
				editedDateView.setVisibility(View.VISIBLE);
				datetimeDividerView.setVisibility(View.VISIBLE);
				editedDateView.setTimestamp(comment.getEditDate());
			}
			commentView.setText(comment.getContent());
		}
	}
}
