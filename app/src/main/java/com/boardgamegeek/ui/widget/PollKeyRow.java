package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ColorUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PollKeyRow extends LinearLayout {
	@BindView(R.id.row_poll_key_view) View colorView;
	@BindView(R.id.row_poll_key_text) TextView textView;
	@BindView(R.id.row_poll_key_info) TextView infoView;

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
		ButterKnife.bind(this);
	}

	public void setColor(int color) {
		ColorUtils.setViewBackground(colorView, color);
	}

	public void setText(CharSequence text) {
		textView.setText(text);
	}

	public void setInfo(CharSequence text) {
		infoView.setText(text);
	}
}
