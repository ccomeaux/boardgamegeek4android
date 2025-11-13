package com.boardgamegeek.ui.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.databinding.RowGeeklistCommentBinding;
import com.boardgamegeek.model.GeekListComment;
import com.boardgamegeek.ui.widget.TimestampView;

import java.util.List;

public class GeekListCommentsRecyclerViewAdapter extends RecyclerView.Adapter<GeekListCommentsRecyclerViewAdapter.CommentViewHolder> {
	private final List<GeekListComment> comments;
	private final LayoutInflater inflater;

	public GeekListCommentsRecyclerViewAdapter(Context context, List<GeekListComment> comments) {
		this.comments = comments;
		inflater = LayoutInflater.from(context);
	}

	@Override
	public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		RowGeeklistCommentBinding binding = RowGeeklistCommentBinding.inflate(inflater, parent, false);
		return new CommentViewHolder(binding);
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
		private final RowGeeklistCommentBinding binding;

		public CommentViewHolder(RowGeeklistCommentBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		public void bind(final GeekListComment comment) {
			binding.username.setText(comment.getUsername());
			binding.numberOfThumbs.setText(String.valueOf(comment.getNumberOfThumbs()));
			binding.postedDate.setTimestamp(comment.getPostDate());
			if (comment.getEditDate() == comment.getPostDate()) {
				binding.editedDate.setVisibility(View.GONE);
				binding.datetimeDivider.setVisibility(View.GONE);
			} else {
				binding.editedDate.setVisibility(View.VISIBLE);
				binding.datetimeDivider.setVisibility(View.VISIBLE);
				binding.editedDate.setTimestamp(comment.getEditDate());
			}
			binding.comment.setText(comment.getContent());
		}
	}
}
