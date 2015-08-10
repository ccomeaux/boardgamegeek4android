package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TableRow;
import android.widget.TextView;

import com.boardgamegeek.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PlayerStatView extends TableRow {
	@InjectView(R.id.name) TextView mName;
	@InjectView(R.id.play_count) TextView mPlayCount;
	@InjectView(R.id.wins) TextView mWins;

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
}
