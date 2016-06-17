package com.boardgamegeek.ui.adapter;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Game.Comment;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.PresentationUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GameCommentsRecyclerViewAdapter extends PaginatedRecyclerViewAdapter<Comment> {
	public GameCommentsRecyclerViewAdapter(Context context, PaginatedData<Comment> data) {
		super(context, R.layout.row_comment, data);
	}

	@NonNull
	@Override
	protected PaginatedItemViewHolder getViewHolder(View itemView) {
		return new CommentViewHolder(itemView);
	}

	public class CommentViewHolder extends PaginatedItemViewHolder {
		@BindView(R.id.username) TextView usernameView;
		@BindView(R.id.rating) TextView ratingView;
		@BindView(R.id.comment) TextView commentView;

		CommentViewHolder(View view) {
			super(view);
			ButterKnife.bind(this, view);
		}

		@Override
		protected void bind(Comment item) {
			usernameView.setText(item.username);
			ratingView.setText(item.getRatingText());
			ColorUtils.setViewBackground(ratingView, ColorUtils.getRatingColor(item.getRating()));
			PresentationUtils.setTextOrHide(commentView, item.value);
		}
	}
}
