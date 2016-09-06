package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

import java.text.DecimalFormat;

import butterknife.BindDimen;
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
	@BindView(R.id.scores) View scoresView;
	@BindView(R.id.score_graph) ScoreGraphView graphView;

	@BindDimen(R.dimen.padding_standard) int standardPadding;

	public PlayerStatView(Context context) {
		super(context);

		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_player_stat, this, true);
		ButterKnife.bind(this);

		setOrientation(LinearLayout.VERTICAL);
		setPadding(0, standardPadding, 0, standardPadding);

		TypedValue background = new TypedValue();
		getContext().getTheme().resolveAttribute(R.attr.selectableItemBackground, background, true);
		setBackgroundResource(background.resourceId);
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

	public void setWinInfo(int wins, int winnableGames) {
		if (wins > winnableGames) winnableGames = wins;
		int winPercentage = 0;
		if (winnableGames > 0) winPercentage = (int) ((double) wins / winnableGames * 100);
		winCountView.setText(getContext().getString(R.string.play_stat_win_percentage, wins, winnableGames, winPercentage));
	}

	public void setWinSkill(int skill) {
		playCountView.setText(String.valueOf(skill));
	}

	public void setOverallLowScore(double score) {
		graphView.setLowScore(score);
	}

	public void setOverallAverageScore(double score) {
		graphView.setAverageScore(score);
	}

	public void setOverallAverageWinScore(double score) {
		graphView.setAverageWinScore(score);
	}

	public void setOverallHighScore(double score) {
		graphView.setHighScore(score);
	}

	public void setLowScore(double score) {
		setScore(lowScoreView, score, Integer.MAX_VALUE);
		graphView.setPersonalLowScore(score);
	}

	public void setAverageScore(double score) {
		setScore(averageScoreView, score, Integer.MIN_VALUE);
		graphView.setPersonalAverageScore(score);
	}

	public void setAverageWinScore(double score) {
		setScore(averageWinScoreView, score, Integer.MIN_VALUE);
		graphView.setPersonalAverageWinScore(score);
	}

	public void setHighScore(double score) {
		setScore(highScoreView, score, Integer.MIN_VALUE);
		graphView.setPersonalHighScore(score);
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
