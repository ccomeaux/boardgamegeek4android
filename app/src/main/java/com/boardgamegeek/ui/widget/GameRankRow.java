package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.PresentationUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GameRankRow extends LinearLayout {
	@BindView(R.id.rank) TextView rankView;
	@BindView(R.id.name) TextView nameView;
	@BindView(R.id.rating) TextView ratingView;

	public GameRankRow(Context context, boolean isFamily) {
		super(context);
		@LayoutRes int layout = isFamily ? R.layout.row_game_rank_family : R.layout.row_game_rank_subtype;
		LayoutInflater.from(context).inflate(layout, this);
		ButterKnife.bind(this);
	}

	public void setRank(int rank) {
		if (PresentationUtils.isRankValid(rank)) {
			rankView.setText(getContext().getString(R.string.rank_prefix, rank));
			rankView.setVisibility(VISIBLE);
		} else {
			rankView.setVisibility(INVISIBLE);
		}
	}

	public void setName(CharSequence name) {
		nameView.setText(name);
	}

	public void setRatingView(double rating) {
		ratingView.setText(PresentationUtils.describeAverageRating(getContext(), rating));
		ColorUtils.setTextViewBackground(ratingView, ColorUtils.getRatingColor(rating));
	}
}
