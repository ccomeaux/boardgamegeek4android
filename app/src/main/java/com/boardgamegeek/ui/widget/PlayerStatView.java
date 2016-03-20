package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TableRow;
import android.widget.TextView;

import com.boardgamegeek.R;

import java.text.DecimalFormat;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PlayerStatView extends TableRow {
	private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.0");

	@InjectView(R.id.name) TextView mName;
	@InjectView(R.id.play_count) TextView mPlayCount;
	@InjectView(R.id.wins) TextView mWins;
	@InjectView(R.id.score) TextView mScore;

	public PlayerStatView(Context context) {
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_player_stat, this);
		ButterKnife.inject(this);
	}

	public void setName(CharSequence text) {
		mName.setText(text);
	}

	public void setPlayCount(int playCount) {
		mPlayCount.setText(String.valueOf(playCount));
	}

	public void setWins(int wins) {
		mWins.setText(String.valueOf(wins));
	}

	public void setAverageScore(double score) {
		mScore.setText(DOUBLE_FORMAT.format(score));
	}
}
