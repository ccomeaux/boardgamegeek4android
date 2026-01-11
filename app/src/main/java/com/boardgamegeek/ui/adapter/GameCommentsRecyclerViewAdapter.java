package com.boardgamegeek.ui.adapter;


import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.databinding.RowCommentBinding;
import com.boardgamegeek.extensions.TextViewUtils;
import com.boardgamegeek.io.model.Game.Comment;
import com.boardgamegeek.ui.model.PaginatedData;
import com.boardgamegeek.util.ColorUtils;

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
		private final RowCommentBinding binding;

		CommentViewHolder(View view) {
			super(view);
			binding = RowCommentBinding.bind(view);
		}

		@Override
		protected void bind(Comment item) {
			binding.username.setText(item.username);
			binding.rating.setText(item.getRatingText());
			ColorUtils.setTextViewBackground(binding.rating, ColorUtils.getRatingColor(item.getRating()));
			TextViewUtils.setTextOrHide(binding.comment, item.value);
		}
	}
}
