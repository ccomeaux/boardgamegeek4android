package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ColorUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class PollKeyRow extends LinearLayout {
	@InjectView(R.id.row_poll_key_view) View mView;
	@InjectView(R.id.row_poll_key_text) TextView mTextView;
	@InjectView(R.id.row_poll_key_info) TextView mInfoView;

	public PollKeyRow(Context context) {
		this(context, null);
	}

	public PollKeyRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		LayoutInflater li = LayoutInflater.from(context);
		li.inflate(R.layout.row_poll_key, this);
		ButterKnife.inject(this);
	}

	public void setColor(int color) {
		ColorUtils.setViewBackground(mView, color);
	}

	public void setText(CharSequence text) {
		mTextView.setText(text);
	}

	public void setInfo(CharSequence text) {
		mInfoView.setText(text);
	}
}
