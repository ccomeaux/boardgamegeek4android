package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

public class PlayerNumberRow extends LinearLayout {
	private PlayerNumberBar mBar;
	private TextView mTextView;

	public PlayerNumberRow(Context context) {
		this(context, null);
	}

	public PlayerNumberRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		LayoutInflater li = LayoutInflater.from(context);
		li.inflate(R.layout.row_poll_players, this);
		mTextView = (TextView) findViewById(R.id.poll_player_text);
		mBar = (PlayerNumberBar) findViewById(R.id.poll_player_bar);
	}

	public void setText(CharSequence text) {
		mTextView.setText(text);
	}

	public void setTotal(int total) {
		mBar.setTotal(total);
	}

	public void setBest(int best) {
		mBar.setBest(best);
	}

	public void setRecommended(int recommended) {
		mBar.setRecommended(recommended);
	}

	public void setNotRecommended(int notRecommended) {
		mBar.setNotRecommended(notRecommended);
	}

	public void setHighlight() {
		mTextView.setBackgroundResource(R.drawable.highlight);
	}

	@SuppressWarnings("deprecation")
	public void clearHighlight() {
		mTextView.setBackgroundDrawable(null);
	}
}
