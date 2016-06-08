package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PlayerStatView extends LinearLayout {
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.0");

	@BindView(R.id.name) TextView nameView;
	@BindView(R.id.play_count) TextView playCountView;
	@BindView(R.id.wins) TextView winCountView;
	@BindView(R.id.score) TextView averageScoreView;
	@BindView(R.id.scores) View scoresView;

	public PlayerStatView(Context context) {
		super(context);
		setOrientation(LinearLayout.VERTICAL);
		int padding = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		setPadding(0, padding, 0, padding);
		int height = getResources().getDimensionPixelSize(R.dimen.view_row_height);
		setMinimumHeight(height);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_player_stat, this, true);
		ButterKnife.bind(this);
	}

	public void showScores(boolean show) {
		scoresView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public String getKey() {
		return nameView.getText().toString();
	}

	public void setName(CharSequence text) {
		nameView.setText(text);
	}

	public void setPlayCount(int playCount) {
		playCountView.setText(String.valueOf(playCount));
	}

	public void setWins(int wins) {
		winCountView.setText(String.valueOf(wins));
	}

	public void setAverageScore(double score) {
		averageScoreView.setText(DOUBLE_FORMAT.format(score));
	}
}
