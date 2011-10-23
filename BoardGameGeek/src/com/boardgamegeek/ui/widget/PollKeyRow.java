package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

public class PollKeyRow extends LinearLayout {

	private View mView;
	private TextView mTextView;

	public PollKeyRow(Context context) {
		this(context, null);
	}

	public PollKeyRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void init(Context context) {
		LayoutInflater li = LayoutInflater.from(context);
		li.inflate(R.layout.row_poll_key, this);
		mView = (View) findViewById(R.id.row_poll_key_view);
		mTextView = (TextView) findViewById(R.id.row_poll_key_text);
	}

	public void setColor(int color) {
		mView.setBackgroundColor(color);
	}

	public void setText(CharSequence text) {
		mTextView.setText(text);
	}
}
