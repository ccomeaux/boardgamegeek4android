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
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.##");

	@BindView(R.id.name) TextView nameView;
	@BindView(R.id.play_count) TextView playCountView;
	@BindView(R.id.wins) TextView winCountView;
	@BindView(R.id.low_score) TextView lowScoreView;
	@BindView(R.id.average_score) TextView averageScoreView;
	@BindView(R.id.average_win_score) TextView averageWinScoreView;
	@BindView(R.id.high_score) TextView highScoreView;
	@BindView(R.id.scores_header) TextView scoresHeader;
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
		scoresHeader.setVisibility(show ? View.VISIBLE : View.GONE);
		scoresView.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public String getKey() {
		return nameView.getText().toString();
	}

	public void setName(CharSequence text) {
		nameView.setText(text);
	}

	public void setWinInfo(int wins, int winnableGames) {
		if (wins > winnableGames) winnableGames = wins;
		int winPercentage = 0;
		if (winnableGames > 0) winPercentage = (int) ((double) wins / winnableGames * 100);
		winCountView.setText(getContext().getString(R.string.play_stat_win_percentage, wins, winnableGames, winPercentage));
	}

	public void setWinSkill(int skill) {
		playCountView.setText(String.valueOf(skill));
	}

	public void setLowScore(double score) {
		setScore(lowScoreView, score, Integer.MAX_VALUE);
	}

	public void setAverageScore(double score) {
		setScore(averageScoreView, score, Integer.MIN_VALUE);
	}

	public void setAverageWinScore(double score) {
		setScore(averageWinScoreView, score, Integer.MIN_VALUE);
	}

	public void setHighScore(double score) {
		setScore(highScoreView, score, Integer.MIN_VALUE);
	}

	private static void setScore(TextView textView, double score, int invalidScore) {
		String text;
		if (score == invalidScore) {
			text = "-";
		} else {
			text = DOUBLE_FORMAT.format(score);
		}
		textView.setText(text);
	}
}
