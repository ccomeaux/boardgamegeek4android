package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
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

	public PlayerStatView(Context context) {
		super(context);
		setOrientation(LinearLayout.HORIZONTAL);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_player_stat, this, true);
		ButterKnife.bind(this);
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
