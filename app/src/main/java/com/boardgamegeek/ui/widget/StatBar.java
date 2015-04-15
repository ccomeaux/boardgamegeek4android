package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

import java.text.NumberFormat;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class StatBar extends FrameLayout {
	private static final NumberFormat FORMAT = NumberFormat.getInstance();
	@InjectView(R.id.value) View mValueView;
	@InjectView(R.id.no_value) View mNoValueView;
	@InjectView(android.R.id.text1) TextView mTextView;

	public StatBar(Context context) {
		this(context, null);
	}

	public StatBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public StatBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		setLayoutParams(new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.MATCH_PARENT));
		setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.stat_bar_height));

		LayoutInflater li = LayoutInflater.from(context);
		li.inflate(R.layout.widget_stat_bar, this, true);

		ButterKnife.inject(this);
	}

	public void setBar(int id, double progress) {
		setBar(id, progress, 10.0);
	}

	public void setBar(int id, double progress, double max) {
		setBar(id, progress, max, 0.0);
	}

	public void setBar(int id, double progress, double max, double min) {
		mTextView.setText(String.format(getContext().getResources().getString(id), FORMAT.format(progress)));
		mValueView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,
			(int) ((progress - min) * 1000)));
		mNoValueView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,
			(int) ((max - progress - min) * 1000)));
	}
}
